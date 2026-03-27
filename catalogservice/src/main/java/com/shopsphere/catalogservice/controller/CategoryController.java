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

    // get all the data of the category and return it to the user
    @Operation(summary = "Get all categories", description = "Fetch all available categories")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categories = categoryService.getAllCategories();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(categories, "Categories fetched successfully"));
    }

    // get the category by id and return it to the user
    @Operation(summary = "Get category by ID", description = "Fetch category details using ID")
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id) {

        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(category, "Category fetched successfully"));
    }

/* ----------------------------------------------Admin Part Start--------------------------------------------------------------------------------------*/
    //Only ADMIN can CREATE Category
    @Operation(summary = "Create category", description = "Admin creates a new category")
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Category created successfully"));
    }

    //Only ADMIN can UPDATE Category
    @Operation(summary = "Update category", description = "Admin updates category details")
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        CategoryResponse updated = categoryService.updateCategory(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(updated, "Category updated successfully"));
    }

    //Only ADMIN can DELETE Category
    @Operation(summary = "Delete category", description = "Admin deletes a category")
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Category deleted successfully"));
    }


    /*-----------------------------------------------------------Homepage Display-----------------------------------------------------------------------------------*/

    /// mapping for accessing the home page - return categories and featuredProduct
    @GetMapping("/public/home")
    public ResponseEntity<ApiResponse<HomepageResponse>> getHomePageData() {
        HomepageResponse homePageResponse = homeService.getHomepageData();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(homePageResponse, "Homepage data fetched successfully"));
    }


}
