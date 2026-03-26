package com.shopsphere.catalogservice.controller;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // PUBLIC → Get product by ID
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse product = productService.getProductById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product fetched successfully"));
    }

    // PUBLIC → Get all products (pagination + sorting)
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {

        PaginationResponse<ProductResponse> products =
                productService.getAllProducts(page, size, sortBy, sortDir);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(products, "Products fetched successfully"));
    }

    // PUBLIC → Search + Filter + Pagination
    @GetMapping("/public/search")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword(keyword);
        filter.setCategoryId(categoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);

        PaginationResponse<ProductResponse> result =
                productService.searchProducts(filter, page, size, sortBy, sortDir);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(result, "Filtered products fetched successfully"));
    }

    // ADMIN → Create product
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    // ADMIN → Update product
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
           @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.updateProduct(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product updated successfully"));
    }

    // ADMIN → Delete product (soft delete)
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Product deleted successfully"));
    }
}