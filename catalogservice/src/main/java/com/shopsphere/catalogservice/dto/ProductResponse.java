package com.shopsphere.catalogservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
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
