package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.OrderAdminDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;


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

    /*
     * What:
     * Returns all orders in admin DTO shape.
     *
     * Why:
     * Adminservice needs full order list for dashboard and order management.
     *
     * How:
     * Implementations fetch latest-first orders and map to OrderAdminDto.
     */
    List<OrderAdminDto> getAllOrdersForAdmin();

    /*
     * What:
     * Returns orders between start and end dates.
     *
     * Why:
     * Admin reports require date-filtered order data.
     *
     * How:
     * Implementations convert date range and fetch matching orders.
     */
    List<OrderAdminDto> getOrdersByDateRange(LocalDate start, LocalDate end);
}