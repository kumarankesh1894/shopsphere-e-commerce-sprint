package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.enums.OrderStatus;

public interface OrderService {
    OrderResponseDto getOrder(Long orderId, Long userId);
    void updateOrderStatus(Long orderId, OrderStatus status);
    void placeOrder(Long orderId, Long userId);
}