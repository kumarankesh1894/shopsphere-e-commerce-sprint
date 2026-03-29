package com.shopsphere.catalogservice.services;

import com.shopsphere.catalogservice.dto.CategoryRequest;
import com.shopsphere.catalogservice.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    // =============================
    // User APIs
    // =============================

    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getAllCategories();

    // =============================
    // Admin APIs
    // =============================

    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);
    void deleteCategory(Long id);
}
