package com.shopsphere.orderservice.client;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "catalogservice",
        fallback = CatalogFallback.class
)
public interface CatalogServiceClient {
    // GET PRODUCT - Returns ApiResponse<ProductResponseDto>
    @GetMapping("/api/catalog/products/public/{id}")
    ApiResponse<ProductResponseDto> getProductById(
            @PathVariable("id") Long productId
    );

    // REDUCE STOCK
    @PutMapping("/api/catalog/products/private/{id}/reducestock")
    void reduceStock(
            @PathVariable("id") Long productId,
            @RequestParam("quantity") Integer quantity
    );
}
