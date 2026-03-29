package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "Checkout APIs", description = "Operations for converting cart into order checkout")
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
    @Operation(summary = "Start checkout", description = "Creates checkout order from the logged-in user's cart and address")
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