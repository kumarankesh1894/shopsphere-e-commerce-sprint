package com.shopsphere.orderservice.dto;

import lombok.*;

import java.math.BigDecimal;
/*
Used for: Order Service → Payment Service (GET order details)

 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentDto {

    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
}
