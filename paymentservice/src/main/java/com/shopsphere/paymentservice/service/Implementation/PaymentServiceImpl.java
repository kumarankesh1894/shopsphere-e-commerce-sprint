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

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final ModelMapper modelMapper;
    /*
     * createRazorpayOrder(...) sends an HTTP request to Razorpay.
     * Instead of creating a brand-new client every time, this line creates one reusable
     * client for the service object.
     *
     * So when payment requests come:
     * request 1 -> uses httpClient
     * request 2 -> uses same httpClient
     * request 3 -> uses same httpClient
     *
     * Why this is better:
     * - less object creation overhead
     * - cleaner code
     * - standard Java practice (HttpClient is safe to reuse)
     *
     * Real-world analogy:
     * Think of HttpClient like a phone used to call Razorpay.
     * Bad approach: buy a new phone for every call.
     * Current approach: keep one phone and use it for all calls.
     */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.currency:INR}")
    private String paymentCurrency;

    // =============================
    // User APIs (called from order flow)
    // =============================

    /*
     * What:
     * Creates or reuses a payment attempt for an order.
     *
     * Why:
     * Order checkout needs an idempotent gateway order creation flow to avoid
     * duplicate payments and keep order/payment state consistent.
     *
     * How:
     * 1) Checks existing payments by orderId and idempotency key.
     * 2) Fetches order details from Order Service when new payment is required.
     * 3) Persists payment row, creates Razorpay order, updates status, and syncs order state.
     * 4) Marks failed state with reason on gateway/processing errors.
     */
    @Transactional(noRollbackFor = PaymentException.class)
    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        if (request == null || request.getOrderId() == null) {
            throw new PaymentException("orderId is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new PaymentException("idempotencyKey is required");
        }

        log.info("payment.create.start orderId={} idemKey={}", request.getOrderId(), request.getIdempotencyKey());

        List<Payment> existingByOrder =
                paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(request.getOrderId());

        if (!existingByOrder.isEmpty()) {
            if (existingByOrder.size() > 1) {
                log.warn("payment.create.duplicate orderId={} rowCount={} action=use_latest",
                        request.getOrderId(),
                        existingByOrder.size());
            }
            Payment payment = existingByOrder.get(0);
            if (shouldReuseByOrder(payment, request.getIdempotencyKey())) {
                log.info("payment.create.idempotent_by_order orderId={} paymentStatus={}", request.getOrderId(), payment.getStatus());
                syncOrderStatus(payment);
                return convertToDto(payment);
            }
            log.info("payment.create.retry_after_failure orderId={} prevStatus={} prevIdemKey={} newIdempotencyKey={}",
                    request.getOrderId(), payment.getStatus(), payment.getIdempotencyKey(), request.getIdempotencyKey());
        }

        // 1. Idempotency check
        List<Payment> existingByKey =
                paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc(request.getIdempotencyKey());

        if (!existingByKey.isEmpty()) {
            if (existingByKey.size() > 1) {
                log.warn("payment.create.duplicate_idem idemKey={} rowCount={} action=use_latest",
                        request.getIdempotencyKey(), existingByKey.size());
            }
            Payment payment = existingByKey.get(0);
            log.info("payment.create.idempotent_by_key idemKey={} paymentStatus={}", request.getIdempotencyKey(), payment.getStatus());
            syncOrderStatus(payment);
            return convertToDto(payment);
        }

        // 2. Fetch order from Order Service
        OrderResponseDto order;
        try {
            order = orderClient.getOrderById(request.getOrderId());
        } catch (RetryableException ex) {
            log.error("payment.create.order_fetch_unavailable orderId=" + request.getOrderId(), ex);
            throw new PaymentException("Order service is temporarily unavailable", ex);
        } catch (FeignException ex) {
            log.error("payment.create.order_fetch_failed orderId=" + request.getOrderId()
                    + " downstreamStatus=" + ex.status(), ex);
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

            log.info("payment.create.gateway_order_created orderId={} razorpayOrderId={}", order.getId(), razorpayOrderId);
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
            log.error("payment.create.failed orderId=" + request.getOrderId() + " reason=" + failureMessage, ex);
            throw new PaymentException("Unable to create Razorpay order: " + failureMessage, ex);
        }
    }

    /*
     * What:
     * Verifies a Razorpay payment signature and finalizes payment status.
     *
     * Why:
     * Payment is trusted only after cryptographic signature verification.
     *
     * How:
     * 1) Locates payment by razorpayOrderId.
     * 2) Short-circuits if payment is already successful (idempotent verify).
     * 3) Validates signature and updates payment/order status to SUCCESS/FAILED.
     */
    @Transactional
    @Override
    public PaymentResponseDto verifyPayment(PaymentVerificationRequestDto request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentVerificationException("Payment not found for Razorpay order"));

        // Idempotency guard: once a payment is marked successful, do not downgrade it.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("payment.verify.idempotent orderId={} paymentStatus={}", payment.getOrderId(), payment.getStatus());
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
                log.error("payment.verify.invalid_signature orderId={} razorpayOrderId={}", payment.getOrderId(), request.getRazorpayOrderId());
                throw new PaymentVerificationException("Invalid Razorpay payment signature");
            }

            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            paymentRepository.save(payment);
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAID);
            log.info("payment.verify.success orderId={} razorpayOrderId={} razorpayPaymentId={}",
                    payment.getOrderId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());

            return convertToDto(payment);
        } catch (RazorpayException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);
            safeUpdateOrderStatus(payment.getOrderId(), OrderStatus.PAYMENT_FAILED);
            log.error("payment.verify.failed orderId=" + payment.getOrderId()
                    + " razorpayOrderId=" + request.getRazorpayOrderId()
                    + " reason=" + ex.getMessage(), ex);
            throw new PaymentVerificationException("Payment verification failed", ex);
        }
    }

    // =============================
    // Internal helper methods
    // =============================

    /*
     * What:
     * Aligns order status with current payment status.
     *
     * Why:
     * Order lifecycle must reflect payment outcome consistently.
     *
     * How:
     * 1) Maps SUCCESS -> PAID.
     * 2) Maps FAILED -> PAYMENT_FAILED.
     * 3) Maps all other in-flight states -> PAYMENT_PENDING.
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

    /*
     * What:
     * Maps Payment entity to API response DTO.
     *
     * Why:
     * Controller/service responses need gateway-safe, frontend-ready fields.
     *
     * How:
     * 1) Uses ModelMapper for baseline field mapping.
     * 2) Sets computed/display fields (status name, gateway, key id, message).
     * 3) Returns response DTO consumed by order/frontend flows.
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

    /*
     * What:
     * Converts rupee amount to paise in integer form.
     *
     * Why:
     * Razorpay create-order API expects amount in the smallest currency unit.
     *
     * How:
     * 1) Shifts decimal two places right.
     * 2) Rounds HALF_UP.
     * 3) Returns exact long value.
     */
    private long toPaise(java.math.BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /*
     * What:
     * Calls Razorpay Orders API to create a gateway order.
     *
     * Why:
     * Frontend checkout requires a valid Razorpay order id before payment popup starts.
     *
     * How:
     * 1) Builds JSON payload with amount/currency/receipt/notes.
     * 2) Sends authenticated HTTP request using key id/secret.
     * 3) Validates 2xx response and parses JSON body.
     */
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

            HttpResponse<String> response = httpClient
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


    /*
     * Reuse an existing payment attempt when it is still in-flight/successful,
     * or when the client retries with the same idempotency key.
     */
    private boolean shouldReuseByOrder(Payment existing, String incomingIdempotencyKey) {
        if (existing.getStatus() == PaymentStatus.SUCCESS
                || existing.getStatus() == PaymentStatus.INITIATED
                || existing.getStatus() == PaymentStatus.PROCESSING) {
            return true;
        }

        return existing.getStatus() == PaymentStatus.FAILED
                && existing.getIdempotencyKey() != null
                && existing.getIdempotencyKey().equals(incomingIdempotencyKey);
    }

    /*
     * What:
     * Updates order status while preventing payment flow failure from downstream errors.
     *
     * Why:
     * Payment record should still be persisted even if order-service sync is temporarily unavailable.
     *
     * How:
     * 1) Calls orderClient.updateOrderStatus(...).
     * 2) Catches and logs exceptions without rethrowing.
     */
    private void safeUpdateOrderStatus(Long orderId, OrderStatus status) {
        try {
            orderClient.updateOrderStatus(orderId, status);
        } catch (Exception ex) {
            log.error("payment.order_status_update.failed orderId=" + orderId + " targetStatus=" + status, ex);
        }
    }
}

