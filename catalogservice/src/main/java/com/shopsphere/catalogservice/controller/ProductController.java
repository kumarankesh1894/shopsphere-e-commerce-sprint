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

    // =============================
    // User APIs
    // =============================

    /*
     * What:
     * Fetches one product by id.
     *
     * Why:
     * Product details page needs full product information for a selected item.
     *
     * How:
     * 1) Reads product id from path.
     * 2) Delegates lookup to productService.getProductById(...).
     * 3) Returns product DTO wrapped in ApiResponse.
     */
    @Operation(summary = "Get product by ID", description = "Fetch a product using its ID (Public API)")
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse product = productService.getProductById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product fetched successfully"));
    }


    /*
     * What:
     * Returns paginated product list with sorting.
     *
     * Why:
     * Product listing pages need controllable pagination and sort options.
     *
     * How:
     * 1) Reads page, size, sortBy, and sortDir query params.
     * 2) Delegates pagination/sorting to productService.getAllProducts(...).
     * 3) Returns page metadata + product items.
     */
    @Operation(summary = "Get all products", description = "Fetch paginated list of products with sorting")
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

    /*
     * What:
     * Searches products using keyword/category/price filters with pagination.
     *
     * Why:
     * Catalog browse flow needs flexible filtering for discovery use cases.
     *
     * How:
     * 1) Builds ProductFilterRequest from query params.
     * 2) Calls productService.searchProducts(...) with paging/sort options.
     * 3) Returns filtered paginated response.
     */
    @Operation(summary = "Search products", description = "Search products using filters like keyword, category, price range")
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

    // =============================
    // Admin APIs
    // =============================

    /*
     * What:
     * Creates a new product record.
     *
     * Why:
     * Admins need a write endpoint to onboard products into catalog.
     *
     * How:
     * 1) Validates request body.
     * 2) Delegates creation to productService.createProduct(...).
     * 3) Returns created product payload with HTTP 201.
     */
    @Operation(summary = "Create product", description = "Admin creates a new product")
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    /*
     * What:
     * Updates an existing product by id.
     *
     * Why:
     * Admins must be able to maintain product metadata and pricing.
     *
     * How:
     * 1) Reads product id and validates update payload.
     * 2) Delegates update to productService.updateProduct(...).
     * 3) Returns updated product response.
     */
    @Operation(summary = "Update product", description = "Admin updates an existing product")
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
           @Valid @RequestBody ProductRequest request) {

        ProductResponse product = productService.updateProduct(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(product, "Product updated successfully"));
    }

    /*
     * What:
     * Soft deletes a product.
     *
     * Why:
     * Admins may need to remove products from active catalog without hard delete.
     *
     * How:
     * 1) Reads product id.
     * 2) Delegates delete behavior to productService.deleteProduct(...).
     * 3) Returns success response with empty payload.
     */
    @Operation(summary = "Delete product", description = "Admin performs soft delete on product")
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Product deleted successfully"));
    }

    /*
     * What:
     * Marks one product as featured.
     *
     * Why:
     * Featured flag is used by homepage and promotional catalog surfaces.
     *
     * How:
     * 1) Reads product id.
     * 2) Delegates feature update to productService.markAsFeatured(...).
     * 3) Returns updated product DTO.
     */
    @Operation(summary = "Mark product as featured", description = "Admin marks a product as featured")
    @PatchMapping("/private/{id}/featured")
    public ResponseEntity<ApiResponse<ProductResponse>> markFeatured(@PathVariable Long id) {
        ProductResponse response = productService.markAsFeatured(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response, "Product marked as featured successfully"));
    }

    // =============================
    // Internal service-to-service APIs
    // =============================

    /*
     * What:
     * Reduces product stock quantity for internal order/payment flow.
     *
     * Why:
     * Inventory should be decremented only after successful payment confirmation.
     *
     * How:
     * 1) Reads product id and quantity.
     * 2) Delegates stock mutation to productService.reduceStock(...).
     * 3) Returns empty success payload.
     */
    @Operation(summary = "Reduce product stock", description = "Reduce stock after order payment")
    @PutMapping("/private/{id}/reducestock")
    public ResponseEntity<ApiResponse<Void>> reduceStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        productService.reduceStock(id, quantity);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Stock reduced successfully")
        );
    }
}