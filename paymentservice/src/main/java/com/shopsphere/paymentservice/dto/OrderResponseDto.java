package com.shopsphere.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderResponseDto {

    private Long id;          // maps to order.getId()
    private Long userId;      // maps to order.getUserId()
    private BigDecimal totalAmount;
}