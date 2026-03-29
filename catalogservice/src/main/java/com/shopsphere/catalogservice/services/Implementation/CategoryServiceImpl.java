package com.shopsphere.catalogservice.services.Implementation;

import com.shopsphere.catalogservice.dto.CategoryRequest;
import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.exception.CategoryNotFoundException;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.services.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j

public class CategoryServiceImpl implements CategoryService {
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;

    // =============================
    // User APIs
    // =============================

    // Get category by ID with exception handling for not found case
    @Override
    public CategoryResponse getCategoryById(Long id) {

        // Log the retrieval attempt for debugging and monitoring
        log.info("Fetching category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    // Log the not found case for better traceability
                    log.warn("Category not found with id: {}", id);
                    return new CategoryNotFoundException("Category not found with id: " + id);
                });

        log.info("Category fetched successfully with id: {}", id);
        return modelMapper.map(category, CategoryResponse.class);
    }

    // =============================
    // Admin APIs
    // =============================

    // Create category with mapping from DTO to Entity and saving to repository
    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        // Log the creation attempt with category name for better traceability
        log.info("Creating category with name: {}", categoryRequest.getName());

        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryRequest.getName())) {
            log.warn("Category already exists with name: {}", categoryRequest.getName());
            throw new IllegalArgumentException("Category already exists with name: " + categoryRequest.getName());
        }

        Category category  = modelMapper.map(categoryRequest, Category.class);

        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully with id: {}", savedCategory.getCategoryId());

        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    // Update category with validation of existence and mapping updated fields from DTO to Entity
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {

        log.info("Updating category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found for update with id: {}", id);
                    return new CategoryNotFoundException("Category not found with id: " + id);
                });

        categoryRepository.findByCategoryNameIgnoreCase(categoryRequest.getName())
                .ifPresent(existing -> {
                    if (!existing.getCategoryId().equals(id)) {
                        log.warn("Category name already in use: {}", categoryRequest.getName());
                        throw new IllegalArgumentException("Category already exists with name: " + categoryRequest.getName());
                    }
                });

        modelMapper.map(categoryRequest, category);

        Category updatedCategory = categoryRepository.save(category);

        log.info("Category updated successfully with id: {}", id);

        return modelMapper.map(updatedCategory, CategoryResponse.class);

    }

    // Delete category with validation of existence and deletion from repository
    @Override
    public void deleteCategory(Long id) {

        log.info("Deleting category with id: {}", id);

        if (!categoryRepository.existsById(id)) {
            log.warn("Category not found for deletion with id: {}", id);
            throw new CategoryNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted successfully with id: {}", id);

    }

    // =============================
    // User APIs
    // =============================

    // Get all categories with mapping from Entity to DTO for each category in the list
    @Override
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");

    List<Category> categories = categoryRepository.findAll();

        log.info("Total categories fetched: {}", categories.size());

        return categories.stream()
                .map(category -> modelMapper.map(category, CategoryResponse.class))
                .toList();
    }
}
