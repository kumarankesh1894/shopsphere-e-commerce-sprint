package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * Internal controller for inter-service communication within the system.
 *
 * This controller exposes endpoints that are intended to be consumed only by
 * other backend services (e.g., Payment Service) and NOT by end users.
 *
 * Responsibilities:
 * - Update order status as part of the order lifecycle
 *   (e.g., PAYMENT_PENDING, PAID, FAILED)
 *
 * Key Characteristics:
 * - Does not require user authentication (no X-UserId)
 * - Uses lightweight responses (no ApiResponse wrapper)
 * - Designed for fast and efficient service-to-service communication
 *
 * Security Note:
 * - These endpoints should be protected via API Gateway, service registry,
 *   or internal network rules to prevent external access.
 *
 * Usage:
 * - Called by Payment Service via Feign client after processing payments
 */
@RestController
@RequestMapping("/api/orders/internal")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok().build();
    }
}