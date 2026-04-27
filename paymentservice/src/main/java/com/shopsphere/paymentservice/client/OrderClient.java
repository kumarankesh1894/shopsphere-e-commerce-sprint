package com.shopsphere.paymentservice.client;

import com.shopsphere.paymentservice.dto.OrderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "orderservice")
public interface OrderClient {

    //  GET order details
    @GetMapping("/api/orders/internal/{orderId}")
    OrderResponseDto getOrderById(@PathVariable("orderId") Long orderId);
}