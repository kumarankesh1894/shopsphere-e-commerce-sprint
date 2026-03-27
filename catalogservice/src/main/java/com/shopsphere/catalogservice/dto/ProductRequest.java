package com.shopsphere.catalogservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String productName;

    private String productDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(0)
    private Integer stock;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotNull(message = "Category Id is required")
    private Long categoryId;
}
