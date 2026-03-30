package com.shopsphere.paymentservice.controller;

import com.shopsphere.paymentservice.dto.PaymentRequestDto;
import com.shopsphere.paymentservice.dto.PaymentResponseDto;
import com.shopsphere.paymentservice.dto.PaymentVerificationRequestDto;
import com.shopsphere.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void createPayment_returnsServiceResponse() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(3L);
        request.setIdempotencyKey("order3_key1");

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setOrderId(3L);
        responseDto.setPaymentStatus("PROCESSING");
        responseDto.setGateway("RAZORPAY");

        when(paymentService.createPayment(request)).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.createPayment(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PROCESSING", response.getBody().getPaymentStatus());
        verify(paymentService).createPayment(request);
    }

    @Test
    void verifyPayment_returnsVerifiedResponse() {
        PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
        request.setRazorpayOrderId("order_abc123");
        request.setRazorpayPaymentId("pay_abc123");
        request.setRazorpaySignature("signature123");

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setOrderId(3L);
        responseDto.setPaymentStatus("SUCCESS");

        when(paymentService.verifyPayment(request)).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.verifyPayment(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("SUCCESS", response.getBody().getPaymentStatus());
        verify(paymentService).verifyPayment(request);
    }
}

