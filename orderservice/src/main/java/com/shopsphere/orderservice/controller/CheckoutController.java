package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkout/private")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    /*
     * What:
     * Starts checkout for the current user and selected cart/request context.
     *
     * Why:
     * Checkout creates a concrete order payload from cart and delivery inputs
     * before payment begins.
     *
     * How:
     * 1) Reads authenticated userId and validates checkout request body.
     * 2) Delegates orchestration to checkoutService.startCheckout(...).
     * 3) Returns order/checkout response details to client.
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<CheckoutResponseDto>> startCheckout(
            @RequestHeader("X-UserId") Long userId,
            @Valid @RequestBody CheckoutRequestDto request
    ) {

        CheckoutResponseDto response =
                checkoutService.startCheckout(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Checkout started successfully")
        );
    }
}