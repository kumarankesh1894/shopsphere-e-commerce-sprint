package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;


public interface OrderService {
    // =============================
    // User APIs
    // =============================

    OrderResponseDto getOrder(Long orderId, Long userId);
    OrderHistoryPageDto getMyOrders(Long userId, int page, int size, String status);
    void cancelOrder(Long orderId, Long userId);

    /*
     * Initiates checkout process for an order.
     *
     * Responsibilities:
     * - Validate order ownership
     * - Transition order from CHECKOUT to initialize Payment state
     * - Trigger Payment Service
     */
    PaymentResponseDto startPayment(Long orderId, Long userId);

    // =============================
    // Admin APIs
    // =============================

    void placeOrder(Long orderId);
    void shipOrder(Long orderId);
    void deliverOrder(Long orderId);
    void cancelOrderAsAdmin(Long orderId);

    // =============================
    // Internal service-to-service APIs
    // =============================

    void updateOrderStatus(Long orderId, OrderStatus status);

    Order getOrderById(Long orderId);
}