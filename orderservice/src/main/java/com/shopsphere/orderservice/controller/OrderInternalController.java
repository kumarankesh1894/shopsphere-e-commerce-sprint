package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.OrderPaymentDto;
import com.shopsphere.orderservice.entity.Order;
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

    /*
     * What:
     * Updates order lifecycle status from trusted internal callers.
     *
     * Why:
     * Payment and other backend services need a direct status update API after
     * async or cross-service processing.
     *
     * How:
     * 1) Reads orderId path variable and target status query param.
     * 2) Delegates transition logic to orderService.updateOrderStatus(...).
     * 3) Returns empty 200 response on success.
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("status") OrderStatus status
    ) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok().build();
    }

    /*
     * What:
     * Returns minimal order payment context for internal payment flow.
     *
     * Why:
     * Payment service requires order amount/user metadata to create and verify
     * payment records.
     *
     * How:
     * 1) Fetches raw order by id via orderService.getOrderById(...).
     * 2) Maps required fields into OrderPaymentDto.
     * 3) Returns lightweight DTO for service-to-service use.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderPaymentDto> getOrderForPayment(
            @PathVariable("orderId") Long orderId) {

        Order order = orderService.getOrderById(orderId);

        OrderPaymentDto dto = OrderPaymentDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .build();

        return ResponseEntity.ok(dto);
    }


}