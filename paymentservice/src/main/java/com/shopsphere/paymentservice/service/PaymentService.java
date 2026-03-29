package com.shopsphere.paymentservice.service;

import com.shopsphere.paymentservice.dto.PaymentRequestDto;
import com.shopsphere.paymentservice.dto.PaymentResponseDto;
import com.shopsphere.paymentservice.dto.PaymentVerificationRequestDto;

public interface PaymentService {

    // =============================
    // User APIs (called from order flow)
    // =============================

    PaymentResponseDto createPayment(PaymentRequestDto request);

    PaymentResponseDto verifyPayment(PaymentVerificationRequestDto request);


    // =============================
    // Admin APIs
    // =============================
    // Currently no admin-only payment service method is implemented.
}