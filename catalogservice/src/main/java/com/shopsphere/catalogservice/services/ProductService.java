package com.shopsphere.catalogservice.services;

import com.shopsphere.catalogservice.dto.PaginationResponse;
import com.shopsphere.catalogservice.dto.ProductFilterRequest;
import com.shopsphere.catalogservice.dto.ProductRequest;
import com.shopsphere.catalogservice.dto.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    // =============================
    // User APIs
    // =============================

    ProductResponse getProductById(Long id);
    PaginationResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    PaginationResponse<ProductResponse> searchProducts(
            ProductFilterRequest filter,
            int page,
            int size,
            String sortBy,
            String sortDir);

    // =============================
    // Admin APIs
    // =============================

    ProductResponse createProduct(ProductRequest productRequest);
    ProductResponse updateProduct(Long id, ProductRequest productRequest);
    void deleteProduct(Long id);
    ProductResponse markAsFeatured(Long id);

    // =============================
    // Internal service-to-service APIs
    // =============================

    void reduceStock(Long productId, Integer quantity);
}
