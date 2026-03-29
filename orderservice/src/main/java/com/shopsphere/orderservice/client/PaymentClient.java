package com.shopsphere.orderservice.client;

import com.shopsphere.orderservice.dto.PaymentRequestDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/*
 * Feign Client to communicate with Payment Service
 *
 * This client is used to:
 * - Trigger payment creation when checkout is initiated
 *
 * Note:
 * - This is an internal service-to-service communication
 * - No authentication headers required
 */
@FeignClient(name = "paymentservice")
public interface PaymentClient {

    @PostMapping("/api/payments/internal")
    PaymentResponseDto createPayment(@RequestBody PaymentRequestDto request);
}