package com.shopsphere.catalogservice.controller;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product APIs", description = "Operations related to products")
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get product by ID", description = "Fetch a product using its ID (Public API)")
    // PUBLIC → Get product by ID
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse product = productService.getProductById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product fetched successfully"));
    }


    @Operation(summary = "Get all products", description = "Fetch paginated list of products with sorting")
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

    @Operation(summary = "Search products", description = "Search products using filters like keyword, category, price range")
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

    @Operation(summary = "Create product", description = "Admin creates a new product")
    // ADMIN → Create product
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @Operation(summary = "Update product", description = "Admin updates an existing product")
    // ADMIN → Update product
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
           @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.updateProduct(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product updated successfully"));
    }

    @Operation(summary = "Delete product", description = "Admin performs soft delete on product")
    // ADMIN → Delete product (soft delete)
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Product deleted successfully"));
    }

    // for featured
    @PatchMapping("/private/{id}/featured")
    public ResponseEntity<ApiResponse<ProductResponse>> markFeatured(@PathVariable Long id) {
        ProductResponse response = productService.markAsFeatured(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response, "Product marked as featured successfully"));
    }
}