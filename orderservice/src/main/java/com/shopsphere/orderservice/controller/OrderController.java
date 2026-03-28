package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/*
 * This controller handles user-facing order operations:
 * - Fetching order details
 * - Placing an order after successful payment
 *
 * These endpoints require authentication and return standardized ApiResponse.
 */
@RestController
@RequestMapping("/api/orders/private")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /*
     * Fetch order details for a specific user.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-UserId") Long userId
    ) {
        OrderResponseDto orderResponse = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        orderResponse,
                        "Order fetched successfully"
                )
        );
    }


    /*
     * Final step of checkout.
     * Allows user to place an order only if payment is successful.
     */
    @PostMapping("/{orderId}/place")
    public ResponseEntity<ApiResponse<Long>> placeOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-UserId") Long userId
    ) {
        orderService.placeOrder(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(orderId,"Order placed successfully"));
    }
}