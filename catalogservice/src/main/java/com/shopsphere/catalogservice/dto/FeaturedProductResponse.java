package com.shopsphere.catalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedProductResponse {

    // these things will be displayed in homepage
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private String categoryName;
}
