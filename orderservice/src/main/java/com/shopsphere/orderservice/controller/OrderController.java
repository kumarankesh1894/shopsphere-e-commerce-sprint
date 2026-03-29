package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * This controller handles order APIs used by users and admins.
 *
 * User APIs:
 * - View own orders
 * - Start payment
 * - Cancel own order (before delivery)
 *
 * Admin APIs:
 * - Mark order as packed, shipped, and delivered
 *
 * All APIs expect identity/role headers forwarded by API Gateway.
 */
@RestController
@RequestMapping("/api/orders/private")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // =============================
    // User APIs
    // =============================

    /*
     * What:
     * Returns paginated order history for the logged-in user.
     *
     * Why:
     * My Orders screen needs a scalable way to fetch recent orders with optional
     * status filtering.
     *
     * How:
     * 1) Reads user identity from X-UserId and pagination/filter query params.
     * 2) Delegates data fetch + filtering to orderService.getMyOrders(...).
     * 3) Wraps result in ApiResponse with pagination metadata.
     */
    @GetMapping("/myorders")
    public ResponseEntity<ApiResponse<OrderHistoryPageDto>> getMyOrders(
            @RequestHeader("X-UserId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        OrderHistoryPageDto orders = orderService.getMyOrders(userId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders, "Order history fetched successfully"));
    }

    /*
     * What:
     * Returns one order details response for the logged-in user.
     *
     * Why:
     * Order details page needs item/address/status information for a single order.
     *
     * How:
     * 1) Reads orderId path variable and X-UserId header.
     * 2) Delegates ownership check + fetch to orderService.getOrder(...).
     * 3) Returns mapped DTO in standard ApiResponse.
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
     * What:
     * Starts payment initiation for a specific order.
     *
     * Why:
     * User-triggered checkout must create a payment request before gateway verify
     * can complete the order lifecycle.
     *
     * How:
     * 1) Reads orderId and authenticated userId from headers/path.
     * 2) Delegates validation + payment request creation to orderService.startPayment(...).
     * 3) Returns payment initiation payload to caller.
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> startPayment(
            @PathVariable Long orderId,
            @RequestHeader("X-UserId") Long userId
    ) {
        PaymentResponseDto response = orderService.startPayment(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment initiated"));
    }

    /*
     * What:
     * Cancels an order for user or admin actor.
     *
     * Why:
     * Cancellation rules differ by role, so the same endpoint routes to role-specific
     * service logic.
     *
     * How:
     * 1) Reads orderId, userId, and role headers.
     * 2) Executes admin or user cancellation branch based on role.
     * 3) Returns cancelled orderId in success response.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Long>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-UserId") Long userId,
            @RequestHeader("X-Role") String role
    ) {
        if (isAdmin(role)) {
            orderService.cancelOrderAsAdmin(orderId);
        } else {
            orderService.cancelOrder(orderId, userId);
        }
        return ResponseEntity.ok(ApiResponse.success(orderId, "Order cancelled successfully"));
    }

    // =============================
    // Admin APIs
    // =============================

    /*
     * What:
     * Marks a PAID order as PACKED.
     *
     * Why:
     * Packing is the first admin fulfillment transition after successful payment.
     *
     * How:
     * 1) Validates admin role via assertAdmin(...).
     * 2) Delegates lifecycle transition to orderService.placeOrder(...).
     * 3) Returns orderId with packed confirmation message.
     */
    @PostMapping("/{orderId}/place")
    public ResponseEntity<ApiResponse<Long>> placeOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.placeOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId,"Order packed successfully"));
    }

    /*
     * What:
     * Marks a PACKED order as SHIPPED.
     *
     * Why:
     * Shipping transition tracks dispatch stage in the order lifecycle.
     *
     * How:
     * 1) Verifies admin role.
     * 2) Calls orderService.shipOrder(...) for transition checks + persistence.
     * 3) Returns success response with orderId.
     */
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<Long>> shipOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.shipOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId, "Order marked as shipped"));
    }

    /*
     * What:
     * Marks a SHIPPED order as DELIVERED.
     *
     * Why:
     * Delivery transition finalizes fulfillment for completed orders.
     *
     * How:
     * 1) Validates admin role.
     * 2) Delegates transition to orderService.deliverOrder(...).
     * 3) Returns orderId with delivered message.
     */
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse<Long>> deliverOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.deliverOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId, "Order marked as delivered"));
    }

    // =============================
    // Internal Helper Methods
    // =============================

    /*
     * What:
     * Enforces admin-only access for protected controller operations.
     *
     * Why:
     * Prevents non-admin callers from executing lifecycle actions like pack/ship/deliver.
     *
     * How:
     * 1) Reuses isAdmin(...) role check.
     * 2) Throws UnauthorizedException when role is not admin.
     */
    private void assertAdmin(String role) {
        if (!isAdmin(role)) {
            throw new UnauthorizedException("Only admin can perform this action");
        }
    }

    /*
     * What:
     * Checks whether incoming role represents admin privileges.
     *
     * Why:
     * Gateway/header values may contain either ADMIN or ROLE_ADMIN naming.
     *
     * How:
     * 1) Performs case-insensitive comparison against accepted admin role values.
     * 2) Returns true for admin, false otherwise.
     */
    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}