package com.shopsphere.paymentservice.client;

import com.shopsphere.paymentservice.dto.OrderResponseDto;
import com.shopsphere.paymentservice.enums.OrderStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "orderservice")
public interface OrderClient {

    //  GET order details
    @GetMapping("/api/orders/internal/{orderId}")
    OrderResponseDto getOrderById(@PathVariable("orderId") Long orderId);

    // UPDATE order status
    @PutMapping("/api/orders/internal/{orderId}/status")
    void updateOrderStatus(@PathVariable("orderId") Long orderId,
                           @RequestParam("status") OrderStatus status);
}