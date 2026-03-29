package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.OrderAdminDto;
import com.shopsphere.orderservice.dto.OrderPaymentDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
@Tag(name = "Order Internal APIs", description = "Internal service-to-service order endpoints")
@RestController
@RequestMapping("/api/orders/internal")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    // =============================
    // Internal APIs for Payment Flow
    // =============================

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
    @Operation(summary = "Update order status", description = "Internal endpoint to update order lifecycle status")
    @PutMapping("/{orderId}/status")
    /*
     * RabbitMQ note:
     * Most status changes come asynchronously from payment events consumed by the listener.
     * This endpoint is the synchronous internal path for trusted services or fallback/manual updates.
     * Both paths use the same service method so status transition rules stay in one place.
     */
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
    @Operation(summary = "Get order payment details", description = "Internal endpoint returning minimal order details required by payment service")
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

    // =============================
    // Internal APIs for Admin Reporting Flow
    // =============================

    @Operation(summary = "Get all orders for admin", description = "Internal endpoint used by admin service to read all orders for dashboard and reports")
    @GetMapping("/admin")
    /*
     * What:
     * Returns all orders in admin-friendly DTO format.
     *
     * Why:
     * Adminservice needs full order list to build dashboard and management views.
     *
     * How:
     * Delegates to orderService.getAllOrdersForAdmin() and returns 200 response.
     */
    public ResponseEntity<List<OrderAdminDto>> getAllOrdersForAdmin() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @Operation(summary = "Get orders by date range for admin", description = "Internal endpoint used by admin service to read orders between two dates")
    @GetMapping("/admin/report")
    /*
     * What:
     * Returns orders inside the provided date range.
     *
     * Why:
     * Adminservice uses this data to build sales/products/customers reports.
     *
     * How:
     * Reads start/end query params and delegates to orderService.getOrdersByDateRange(...).
     */
    public ResponseEntity<List<OrderAdminDto>> getOrdersByDateRange(
            @RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end) {
        return ResponseEntity.ok(orderService.getOrdersByDateRange(start, end));
    }


}