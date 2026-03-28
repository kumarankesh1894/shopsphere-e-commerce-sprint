package com.shopsphere.orderservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer stock;
}