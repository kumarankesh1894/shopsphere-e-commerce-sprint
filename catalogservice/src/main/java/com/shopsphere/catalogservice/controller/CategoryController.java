package com.shopsphere.catalogservice.controller;


import com.shopsphere.catalogservice.dto.ApiResponse;
import com.shopsphere.catalogservice.dto.CategoryRequest;
import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog/categories")
public class CategoryController {
        private final CategoryService categoryService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> categories = categoryService.getAllCategories();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(categories, "Categories fetched successfully"));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id) {

        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(category, "Category fetched successfully"));
    }

    //Only ADMIN can CREATE Category
    @PostMapping("/private")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Category created successfully"));
    }

    //Only ADMIN can UPDATE Category
    @PutMapping("/private/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        CategoryResponse updated = categoryService.updateCategory(id, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(updated, "Category updated successfully"));
    }

    //Only ADMIN can DELETE Category
    @DeleteMapping("/private/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Category deleted successfully"));
    }
}
