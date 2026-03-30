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
import com.shopsphere.paymentservice.messaging.OrderStatusEventPublisher;
import com.shopsphere.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
}

