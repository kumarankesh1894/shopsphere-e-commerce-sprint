package com.shopsphere.catalogservice.controller;


import com.shopsphere.catalogservice.dto.ApiResponse;
import com.shopsphere.catalogservice.dto.CategoryRequest;
import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.dto.HomepageResponse;
import com.shopsphere.catalogservice.services.CategoryService;
import com.shopsphere.catalogservice.services.HomepageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category APIs", description = "Operations related to categories")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog/categories")
public class CategoryController {
        private final CategoryService categoryService;
        private final HomepageService homeService;

    // =============================
    // User APIs
    // =============================

    /*
     * What:
     * Returns all catalog categories.
     *
     * Why:
     * Category listing is needed for navigation, filtering, and homepage sections.
     *
     * How:
     * 1) Calls categoryService.getAllCategories(...).
     * 2) Wraps result list in ApiResponse.
     * 3) Returns HTTP 200.
     */
    @Operation(summary = "Get all categories", description = "Fetch all available categories")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categories = categoryService.getAllCategories();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(categories, "Categories fetched successfully"));
    }

    /*
     * What:
     * Fetches one category by id.
     *
     * Why:
     * Category details endpoint supports targeted views and admin checks.
     *
     * How:
     * 1) Reads category id from path.
     * 2) Delegates lookup to categoryService.getCategoryById(...).
     * 3) Returns category DTO in ApiResponse.
     */
    @Operation(summary = "Get category by ID", description = "Fetch category details using ID")
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id) {

        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(category, "Category fetched successfully"));
    }

    // =============================
    // Admin APIs
    // =============================

    /*
     * What:
     * Creates a new category.
     *
     * Why:
     * Admins need to extend catalog taxonomy as product domains evolve.
     *
     * How:
     * 1) Reads category payload from request body.
     * 2) Delegates creation to categoryService.createCategory(...).
     * 3) Returns created category response with HTTP 201.
     */
    @Operation(summary = "Create category", description = "Admin creates a new category")
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Category created successfully"));
    }

    /*
     * What:
     * Updates category metadata for an existing category id.
     *
     * Why:
     * Admins need to correct and maintain category names/details over time.
     *
     * How:
     * 1) Reads id + update payload.
     * 2) Delegates update logic to categoryService.updateCategory(...).
     * 3) Returns updated category DTO.
     */
    @Operation(summary = "Update category", description = "Admin updates category details")
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        CategoryResponse updated = categoryService.updateCategory(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(updated, "Category updated successfully"));
    }

    /*
     * What:
     * Deletes an existing category.
     *
     * Why:
     * Admins need cleanup operations for unused or merged categories.
     *
     * How:
     * 1) Reads category id.
     * 2) Delegates delete operation to categoryService.deleteCategory(...).
     * 3) Returns success response with null payload.
     */
    @Operation(summary = "Delete category", description = "Admin deletes a category")
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Category deleted successfully"));
    }

    /*
     * What:
     * Returns homepage data bundle (categories + featured products).
     *
     * Why:
     * Home screen needs a single aggregated payload for first-load performance.
     *
     * How:
     * 1) Calls homeService.getHomepageData(...).
     * 2) Wraps aggregate response in ApiResponse.
     * 3) Returns HTTP 200 with homepage payload.
     */
    @GetMapping("/public/home")
    public ResponseEntity<ApiResponse<HomepageResponse>> getHomePageData() {
        HomepageResponse homePageResponse = homeService.getHomepageData();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(homePageResponse, "Homepage data fetched successfully"));
    }


}
