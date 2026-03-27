package com.shopsphere.catalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private String productName;
    private String productDescription;
    private String imageUrl;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private Boolean isAvailable;
}
