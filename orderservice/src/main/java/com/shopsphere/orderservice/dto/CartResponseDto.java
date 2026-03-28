package com.shopsphere.orderservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDto {

    private List<CartItemResponseDto> items;
    private BigDecimal totalAmount;
}