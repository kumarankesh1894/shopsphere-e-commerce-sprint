package com.shopsphere.paymentservice.controller;

import com.shopsphere.paymentservice.dto.PaymentRequestDto;
import com.shopsphere.paymentservice.dto.PaymentResponseDto;
import com.shopsphere.paymentservice.dto.PaymentVerificationRequestDto;
import jakarta.validation.Valid;
import com.shopsphere.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/internal")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request) {

        return ResponseEntity.ok(
                paymentService.createPayment(request)
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequestDto request) {

        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }
}