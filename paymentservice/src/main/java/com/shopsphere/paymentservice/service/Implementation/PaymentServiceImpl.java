package com.shopsphere.paymentservice.service.Implementation;

import com.shopsphere.paymentservice.client.OrderClient;
import com.shopsphere.paymentservice.dto.*;
import com.shopsphere.paymentservice.entity.Payment;
import com.shopsphere.paymentservice.enums.Gateway;
import com.shopsphere.paymentservice.enums.OrderStatus;
import com.shopsphere.paymentservice.enums.PaymentStatus;
import com.shopsphere.paymentservice.exception.PaymentException;
import com.shopsphere.paymentservice.exception.PaymentVerificationException;
import com.shopsphere.paymentservice.repository.PaymentRepository;
import com.shopsphere.paymentservice.service.PaymentService;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final ModelMapper modelMapper;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.currency:INR}")
    private String paymentCurrency;

    @Transactional(noRollbackFor = PaymentException.class)
    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        log.info("Creating payment for order {}", request.getOrderId());

        List<Payment> existingByOrder =
                paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(request.getOrderId());

        if (!existingByOrder.isEmpty()) {
            if (existingByOrder.size() > 1) {
                log.warn("Found {} payment rows for order {}. Using the latest row.",
                        existingByOrder.size(), request.getOrderId());
            }
            Payment payment = existingByOrder.get(0);
            syncOrderStatus(payment);
            return convertToDto(payment);
        }

        // 1. Idempotency check
        List<Payment> existingByKey =
                paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc(request.getIdempotencyKey());

        if (!existingByKey.isEmpty()) {
            if (existingByKey.size() > 1) {
                log.warn("Found {} payment rows for idempotencyKey {}. Using the latest row.",
                        existingByKey.size(), request.getIdempotencyKey());
            }
            Payment payment = existingByKey.get(0);
            syncOrderStatus(payment);
            return convertToDto(payment);
        }

        // 2. Fetch order from Order Service
        OrderResponseDto order;
        try {
            order = orderClient.getOrderById(request.getOrderId());
        } catch (RetryableException ex) {
            log.error("Order service is unavailable while creating payment for order {}", request.getOrderId(), ex);
            throw new PaymentException("Order service is temporarily unavailable", ex);
        } catch (FeignException ex) {
            log.error("Failed to fetch order {} from order service. Downstream status: {}",
                    request.getOrderId(), ex.status(), ex);
            throw ex;
        }

        if (order == null) {
            throw new PaymentException("Order not found");
        }

        // 3. Create payment record first for audit and idempotency
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setGateway(Gateway.RAZORPAY);
        payment.setCurrency(paymentCurrency);
        payment.setAmountInPaise(toPaise(order.getTotalAmount()));

        payment = paymentRepository.save(payment);

        try {
            // 4. Create Razorpay order for client-side checkout.
            JSONObject razorpayOrder = createRazorpayOrder(payment, request, order);
            String razorpayOrderId = razorpayOrder.optString("id", null);

            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                throw new PaymentException("Razorpay order id missing in response");
            }

            log.info("Razorpay order created: {}", razorpayOrderId);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // 5. Order is waiting for payment confirmation from Razorpay callback/verify flow.
            safeUpdateOrderStatus(order.getId(), OrderStatus.PAYMENT_PENDING);
            return convertToDto(payment);
        } catch (Exception ex) {
            String failureMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown payment gateway error";
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureMessage);
            paymentRepository.save(payment);
            safeUpdateOrderStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
            log.error("Payment failed while creating Razorpay order for order {}", request.getOrderId(), ex);
            throw new PaymentException("Unable to create Razorpay order: " + failureMessage, ex);
        }
    }

    @Transactional
    @Override
    public PaymentResponseDto verifyPayment(PaymentVerificationRequestDto request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentVerificationException("Payment not found for Razorpay order"));

        // Idempotency guard: once a payment is marked successful, do not downgrade it.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return convertToDto(payment);
        }

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            if (!valid) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Invalid Razorpay signature");
                paymentRepository.save(payment);
                safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED);
                log.error("Payment verification failed for order {} due to invalid signature", payment.getOrderId());
                throw new PaymentVerificationException("Invalid Razorpay payment signature");
            }

            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            paymentRepository.save(payment);
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAID);

            return convertToDto(payment);
        } catch (RazorpayException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED);
            log.error("Payment verification failed for order {}", payment.getOrderId(), ex);
            throw new PaymentVerificationException("Payment verification failed", ex);
        }
    }

    /**
     * Sync Order Status with Payment Status
     */
    private void syncOrderStatus(Payment payment) {

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAID);

        } else if (payment.getStatus() == PaymentStatus.FAILED) {
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED);

        } else {
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_PENDING);
        }
    }

    /**
     * Convert Entity → DTO
     */
    private PaymentResponseDto convertToDto(Payment payment) {
        PaymentResponseDto dto =
                modelMapper.map(payment, PaymentResponseDto.class);
        dto.setPaymentStatus(payment.getStatus().name());
        dto.setGateway(payment.getGateway() != null ? payment.getGateway().name() : null);
        dto.setRazorpayKeyId(razorpayKeyId);
        dto.setFailureReason(payment.getFailureReason());
        dto.setMessage("Use razorpayOrderId and razorpayKeyId on frontend checkout.");
        return dto;
    }

    private long toPaise(java.math.BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private JSONObject createRazorpayOrder(Payment payment, PaymentRequestDto request, OrderResponseDto order) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("amount", payment.getAmountInPaise());
            payload.put("currency", paymentCurrency);
            payload.put("receipt", request.getIdempotencyKey());
            payload.put("notes", new JSONObject().put("orderId", order.getId()));

            String credentials = razorpayKeyId + ":" + razorpayKeySecret;
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentException("Razorpay API error " + response.statusCode() + ": " + response.body());
            }

            return new JSONObject(response.body());
        } catch (PaymentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentException("Failed to call Razorpay create order API", ex);
        }
    }

    private void safeUpdateOrderStatus(Long orderId, OrderStatus status) {
        try {
            orderClient.updateOrderStatus(orderId, status);
        } catch (Exception ex) {
            log.error("Unable to update order {} status to {}", orderId, status, ex);
        }
    }
}