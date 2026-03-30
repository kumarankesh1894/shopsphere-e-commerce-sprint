package com.shopsphere.catalogservice.services.Implementation;

import com.shopsphere.catalogservice.dto.CategoryRequest;
import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.exception.CategoryNotFoundException;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createCategory_whenDuplicateName_throwsIllegalArgumentException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByCategoryNameIgnoreCase("Electronics")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_whenNameTakenByAnotherCategory_throwsIllegalArgumentException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        Category existing = new Category();
        existing.setCategoryId(1L);

        Category conflicting = new Category();
        conflicting.setCategoryId(2L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByCategoryNameIgnoreCase("Electronics")).thenReturn(Optional.of(conflicting));

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void getCategoryById_whenFound_returnsMappedResponse() {
        Category category = new Category();
        category.setCategoryId(3L);

        CategoryResponse response = new CategoryResponse();
        response.setId(3L);

        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(modelMapper.map(category, CategoryResponse.class)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryById(3L);

        assertEquals(3L, result.getId());
    }

    @Test
    void deleteCategory_whenNotFound_throwsCategoryNotFoundException() {
        when(categoryRepository.existsById(11L)).thenReturn(false);

        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(11L));
        verify(categoryRepository, never()).deleteById(11L);
    }
}

