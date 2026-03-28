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