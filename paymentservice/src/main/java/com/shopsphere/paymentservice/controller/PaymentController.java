package com.shopsphere.paymentservice.controller;

import com.shopsphere.paymentservice.dto.PaymentRequestDto;
import com.shopsphere.paymentservice.dto.PaymentResponseDto;
import com.shopsphere.paymentservice.dto.PaymentVerificationRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.shopsphere.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment APIs", description = "Internal payment creation and verification operations")
@RestController
@RequestMapping("/api/payments/internal")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // =============================
    // User APIs (called from order flow)
    // =============================

    /*
     * What:
     * Creates a new payment request for a given order.
     *
     * Why:
     * Order service calls this when user starts payment, so backend can
     * create and track one payment entry before verification.
     *
     * How:
     * 1) Reads and validates orderId + idempotencyKey from request body.
     * 2) Logs request details for troubleshooting.
     * 3) Delegates business logic to paymentService.createPayment(...).
     * 4) Returns payment initiation details (gateway order id, key id, status).
     */
    @Operation(summary = "Create payment", description = "Creates a payment entry and gateway order for the requested order")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request) {
        log.info("payment.controller.create.request orderId={}", request.getOrderId());

        return ResponseEntity.ok(
                paymentService.createPayment(request)
        );
    }

    /*
     * What:
     * Verifies the payment using gateway signature and marks final status.
     *
     * Why:
     * Payment is not trusted until signature verification is successful.
     * This step confirms payment authenticity before order is marked PAID.
     *
     * How:
     * 1) Reads and validates razorpayOrderId, paymentId, and signature.
     * 2) Logs verify request with order reference.
     * 3) Delegates verification to paymentService.verifyPayment(...).
     * 4) Returns updated payment state after verification logic.
     */
    @Operation(summary = "Verify payment", description = "Verifies gateway signature and updates final payment status")
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequestDto request) {
        log.info("payment.controller.verify.request razorpayOrderId={}", request.getRazorpayOrderId());

        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }


    // =============================
    // Admin APIs
    // =============================
    // Currently no admin-only payment endpoint is implemented.
}