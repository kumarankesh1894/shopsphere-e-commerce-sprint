package com.shopsphere.orderservice.client;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.ProductResponseDto;
import org.springframework.stereotype.Component;

@Component
public class CatalogFallback implements CatalogServiceClient {

    @Override
    public ApiResponse<ProductResponseDto> getProductById(Long productId) {
        throw new RuntimeException("Catalog Service is down. Please try again later.");
    }

    @Override
    public void reduceStock(Long productId, Integer quantity) {
        throw new RuntimeException("Unable to reduce stock. Catalog Service unavailable.");
    }
}