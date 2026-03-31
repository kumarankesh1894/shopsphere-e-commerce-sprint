package com.shopsphere.paymentservice.service.Implementation;

import com.shopsphere.paymentservice.client.OrderClient;
import com.shopsphere.paymentservice.dto.OrderStatusUpdateEvent;
import com.shopsphere.paymentservice.dto.PaymentRequestDto;
import com.shopsphere.paymentservice.dto.PaymentResponseDto;
import com.shopsphere.paymentservice.dto.PaymentVerificationRequestDto;
import com.shopsphere.paymentservice.entity.Payment;
import com.shopsphere.paymentservice.enums.Gateway;
import com.shopsphere.paymentservice.enums.OrderStatus;
import com.shopsphere.paymentservice.enums.PaymentStatus;
import com.shopsphere.paymentservice.exception.PaymentException;
import com.shopsphere.paymentservice.exception.PaymentVerificationException;
import com.shopsphere.paymentservice.messaging.OrderStatusEventPublisher;
import com.shopsphere.paymentservice.repository.PaymentRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private OrderStatusEventPublisher orderStatusEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "secret");
        ReflectionTestUtils.setField(paymentService, "paymentCurrency", "INR");
    }

    @Test
    void createPayment_whenRequestIsNull_throwsPaymentException() {
        assertThrows(PaymentException.class, () -> paymentService.createPayment(null));
    }

    @Test
    void createPayment_whenIdempotencyKeyMissing_throwsPaymentException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setIdempotencyKey("  ");

        assertThrows(PaymentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenOrderIdMissing_throwsPaymentException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setIdempotencyKey("idem-1");

        assertThrows(PaymentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenExistingProcessingPayment_reusesAndPublishesPending() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(3L);
        request.setIdempotencyKey("k1");

        Payment existing = Payment.builder()
                .id(9L)
                .orderId(3L)
                .userId(2L)
                .status(PaymentStatus.PROCESSING)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .amountInPaise(250000L)
                .createdAt(LocalDateTime.now())
                .idempotencyKey("old_key")
                .build();

        PaymentResponseDto mapped = new PaymentResponseDto();
        mapped.setOrderId(3L);
        mapped.setPaymentStatus("PROCESSING");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(3L)).thenReturn(List.of(existing));
        when(modelMapper.map(existing, PaymentResponseDto.class)).thenReturn(mapped);

        PaymentResponseDto response = paymentService.createPayment(request);

        assertEquals(3L, response.getOrderId());
        assertEquals("PROCESSING", response.getPaymentStatus());
        verify(orderStatusEventPublisher).publish(any(OrderStatusUpdateEvent.class));
        verify(orderClient, never()).getOrderById(any());
    }

    @Test
    void createPayment_whenExistingInitiatedPayment_reusesAndPublishesPending() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(31L);
        request.setIdempotencyKey("k31");

        Payment existing = Payment.builder()
                .id(31L)
                .orderId(31L)
                .userId(9L)
                .status(PaymentStatus.INITIATED)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .idempotencyKey("old-k")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto mapped = new PaymentResponseDto();
        mapped.setOrderId(31L);
        mapped.setPaymentStatus("INITIATED");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(31L)).thenReturn(List.of(existing));
        when(modelMapper.map(existing, PaymentResponseDto.class)).thenReturn(mapped);

        PaymentResponseDto response = paymentService.createPayment(request);

        assertEquals(31L, response.getOrderId());
        assertEquals("INITIATED", response.getPaymentStatus());
        verify(orderStatusEventPublisher).publish(any(OrderStatusUpdateEvent.class));
        verify(orderClient, never()).getOrderById(any());
    }

    @Test
    void createPayment_whenExistingByIdempotencyKey_reusesLatest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(99L);
        request.setIdempotencyKey("same-key");

        Payment byKey = Payment.builder()
                .orderId(42L)
                .status(PaymentStatus.FAILED)
                .idempotencyKey("same-key")
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(42L);
        dto.setPaymentStatus("FAILED");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(99L)).thenReturn(List.of());
        when(paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc("same-key")).thenReturn(List.of(byKey));
        when(modelMapper.map(byKey, PaymentResponseDto.class)).thenReturn(dto);

        PaymentResponseDto response = paymentService.createPayment(request);

        assertEquals(42L, response.getOrderId());
        assertEquals("FAILED", response.getPaymentStatus());
        verify(orderStatusEventPublisher).publish(any(OrderStatusUpdateEvent.class));
    }

    @Test
    void createPayment_whenOrderResponseIsNull_throwsPaymentException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(17L);
        request.setIdempotencyKey("k17");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(17L)).thenReturn(List.of());
        when(paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc("k17")).thenReturn(List.of());
        when(orderClient.getOrderById(17L)).thenReturn(null);

        assertThrows(PaymentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenOrderFetchRetryable_throwsPaymentException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(44L);
        request.setIdempotencyKey("k44");

        RetryableException retryable = org.mockito.Mockito.mock(RetryableException.class);

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(44L)).thenReturn(List.of());
        when(paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc("k44")).thenReturn(List.of());
        when(orderClient.getOrderById(44L)).thenThrow(retryable);

        assertThrows(PaymentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenOrderFetchFeignException_rethrowsFeignException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(45L);
        request.setIdempotencyKey("k45");

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/orders/45", Map.of(), null, null, null);
        Response feignResponse = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(feignRequest)
                .headers(Map.of())
                .build();
        FeignException feignException = FeignException.errorStatus("OrderClient#getOrderById", feignResponse);

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(45L)).thenReturn(List.of());
        when(paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc("k45")).thenReturn(List.of());
        when(orderClient.getOrderById(45L)).thenThrow(feignException);

        assertThrows(FeignException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenFailedAndSameIdempotency_reusesExistingFailedPayment() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(46L);
        request.setIdempotencyKey("same-key");

        Payment failed = Payment.builder()
                .orderId(46L)
                .status(PaymentStatus.FAILED)
                .idempotencyKey("same-key")
                .failureReason("previous failure")
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(46L);
        dto.setPaymentStatus("FAILED");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(46L)).thenReturn(List.of(failed));
        when(modelMapper.map(failed, PaymentResponseDto.class)).thenReturn(dto);

        PaymentResponseDto response = paymentService.createPayment(request);

        assertEquals("FAILED", response.getPaymentStatus());
        verify(orderClient, never()).getOrderById(any());
    }

    @Test
    void createPayment_whenExistingFailedWithDifferentIdempotency_andOrderClientRetryable_throwsPaymentException() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(48L);
        request.setIdempotencyKey("new-key");

        Payment failed = Payment.builder()
                .orderId(48L)
                .status(PaymentStatus.FAILED)
                .idempotencyKey("old-key")
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        RetryableException retryable = org.mockito.Mockito.mock(RetryableException.class);

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(48L)).thenReturn(List.of(failed));
        when(paymentRepository.findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc("new-key")).thenReturn(List.of());
        when(orderClient.getOrderById(48L)).thenThrow(retryable);

        assertThrows(PaymentException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenIdempotentAndPublisherFails_stillReturnsResponse() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(47L);
        request.setIdempotencyKey("k47");

        Payment existing = Payment.builder()
                .orderId(47L)
                .status(PaymentStatus.PROCESSING)
                .idempotencyKey("k47")
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(47L);
        dto.setPaymentStatus("PROCESSING");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(47L)).thenReturn(List.of(existing));
        when(modelMapper.map(existing, PaymentResponseDto.class)).thenReturn(dto);
        doThrow(new RuntimeException("broker down"))
                .when(orderStatusEventPublisher)
                .publish(any(OrderStatusUpdateEvent.class));

        PaymentResponseDto response = paymentService.createPayment(request);

        assertEquals("PROCESSING", response.getPaymentStatus());
    }

    @Test
    void verifyPayment_whenAlreadySuccess_returnsWithoutChangingStatus() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId("order_1");
        request.setRazorpayPaymentId("pay_1");
        request.setRazorpaySignature("sig_1");

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(5L)
                .status(PaymentStatus.SUCCESS)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(5L);
        dto.setPaymentStatus("SUCCESS");

        when(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, PaymentResponseDto.class)).thenReturn(dto);

        PaymentResponseDto response = paymentService.verifyPayment(request);

        assertEquals("SUCCESS", response.getPaymentStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void verifyPayment_whenPaymentMissing_throwsPaymentVerificationException() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId("missing");
        request.setRazorpayPaymentId("pay_x");
        request.setRazorpaySignature("sig_x");

        when(paymentRepository.findByRazorpayOrderId("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentVerificationException.class, () -> paymentService.verifyPayment(request));
    }

    @Test
    void verifyPayment_whenInvalidSignature_marksFailedPublishesPaymentFailedAndThrows() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId("order_invalid");
        request.setRazorpayPaymentId("pay_invalid");
        request.setRazorpaySignature("definitely_invalid_signature");

        Payment payment = Payment.builder()
                .id(2L)
                .orderId(50L)
                .status(PaymentStatus.PROCESSING)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_invalid")).thenReturn(Optional.of(payment));

        assertThrows(PaymentVerificationException.class, () -> paymentService.verifyPayment(request));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());
        assertEquals("Invalid Razorpay signature", paymentCaptor.getValue().getFailureReason());

        ArgumentCaptor<OrderStatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusUpdateEvent.class);
        verify(orderStatusEventPublisher).publish(eventCaptor.capture());
        assertEquals(String.valueOf(OrderStatus.PAYMENT_FAILED), eventCaptor.getValue().getStatus());
    }

    @Test
    void verifyPayment_whenInvalidSignatureAndPublisherFails_stillThrowsAndSavesFailedPayment() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId("order_pub_fail");
        request.setRazorpayPaymentId("pay_pub_fail");
        request.setRazorpaySignature("still_invalid_signature");

        Payment payment = Payment.builder()
                .id(3L)
                .orderId(51L)
                .status(PaymentStatus.PROCESSING)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_pub_fail")).thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("broker down"))
                .when(orderStatusEventPublisher)
                .publish(any(OrderStatusUpdateEvent.class));

        PaymentVerificationException ex = assertThrows(
                PaymentVerificationException.class,
                () -> paymentService.verifyPayment(request)
        );

        assertTrue(ex.getMessage().contains("Invalid Razorpay payment signature"));
        verify(paymentRepository).save(any(Payment.class));
        verify(orderStatusEventPublisher).publish(any(OrderStatusUpdateEvent.class));
    }

    @Test
    void verifyPayment_whenValidSignature_marksSuccessAndPublishesPaid() throws Exception {
        String razorpayOrderId = "order_valid_1";
        String razorpayPaymentId = "pay_valid_1";

        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId(razorpayOrderId);
        request.setRazorpayPaymentId(razorpayPaymentId);
        request.setRazorpaySignature(createValidRazorpaySignature(razorpayOrderId, razorpayPaymentId, "secret"));

        Payment payment = Payment.builder()
                .id(4L)
                .orderId(60L)
                .status(PaymentStatus.PROCESSING)
                .failureReason("transient issue")
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto mapped = new PaymentResponseDto();
        mapped.setOrderId(60L);

        when(paymentRepository.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, PaymentResponseDto.class)).thenReturn(mapped);

        PaymentResponseDto response = paymentService.verifyPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.SUCCESS, paymentCaptor.getValue().getStatus());
        assertEquals(razorpayPaymentId, paymentCaptor.getValue().getTransactionId());
        assertNull(paymentCaptor.getValue().getFailureReason());

        ArgumentCaptor<OrderStatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusUpdateEvent.class);
        verify(orderStatusEventPublisher).publish(eventCaptor.capture());
        assertEquals(String.valueOf(OrderStatus.PAID), eventCaptor.getValue().getStatus());

        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals(60L, response.getOrderId());
    }

    @Test
    void verifyPayment_whenValidSignatureAndPublisherFails_stillReturnsSuccess() throws Exception {
        String razorpayOrderId = "order_valid_2";
        String razorpayPaymentId = "pay_valid_2";

        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId(razorpayOrderId);
        request.setRazorpayPaymentId(razorpayPaymentId);
        request.setRazorpaySignature(createValidRazorpaySignature(razorpayOrderId, razorpayPaymentId, "secret"));

        Payment payment = Payment.builder()
                .id(5L)
                .orderId(61L)
                .status(PaymentStatus.PROCESSING)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto mapped = new PaymentResponseDto();
        mapped.setOrderId(61L);

        when(paymentRepository.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, PaymentResponseDto.class)).thenReturn(mapped);
        doThrow(new RuntimeException("broker down"))
                .when(orderStatusEventPublisher)
                .publish(any(OrderStatusUpdateEvent.class));

        PaymentResponseDto response = paymentService.verifyPayment(request);

        assertEquals("SUCCESS", response.getPaymentStatus());
        verify(paymentRepository).save(any(Payment.class));
        verify(orderStatusEventPublisher).publish(any(OrderStatusUpdateEvent.class));
    }

    @Test
    void createPayment_whenExistingSuccess_publishesPaidStatusEvent() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(77L);
        request.setIdempotencyKey("retry-key");

        Payment existing = Payment.builder()
                .orderId(77L)
                .status(PaymentStatus.SUCCESS)
                .gateway(Gateway.RAZORPAY)
                .currency("INR")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(77L);
        dto.setPaymentStatus("SUCCESS");

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDescIdDesc(77L)).thenReturn(List.of(existing));
        when(modelMapper.map(existing, PaymentResponseDto.class)).thenReturn(dto);

        paymentService.createPayment(request);

        ArgumentCaptor<OrderStatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusUpdateEvent.class);
        verify(orderStatusEventPublisher).publish(eventCaptor.capture());
        assertEquals(String.valueOf(OrderStatus.PAID), eventCaptor.getValue().getStatus());
    }

    private String createValidRazorpaySignature(String razorpayOrderId, String razorpayPaymentId, String secret)
            throws Exception {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

