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
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.placeOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId,"Order packed successfully"));
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<Long>> shipOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.shipOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId, "Order marked as shipped"));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<ApiResponse<Long>> deliverOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-Role") String role
    ) {
        assertAdmin(role);
        orderService.deliverOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(orderId, "Order marked as delivered"));
    }

    /*
     * Checkout API
     *
     * This endpoint is triggered when a user clicks the "Checkout" button.
     * It initiates the payment process for the given order.
     *
     * Flow:
     * - Validates user ownership of the order
     * - Moves order status to CHECKOUT
     * - Calls Payment Service to process payment
     *
     * Note:
     * - Requires authentication via X-UserId header
     * - Returns a simple success message (payment happens asynchronously inside service)
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> startPayment(
            @PathVariable Long orderId,
            @RequestHeader("X-UserId") Long userId
    ) {
        PaymentResponseDto response = orderService.startPayment(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment initiated"));
    }

    private void assertAdmin(String role) {
        if (!("ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role))) {
            throw new UnauthorizedException("Only admin can perform this action");
        }
    }
}