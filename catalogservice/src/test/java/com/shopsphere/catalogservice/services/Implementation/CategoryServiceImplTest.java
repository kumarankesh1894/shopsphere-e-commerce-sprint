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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    void createCategory_whenValid_savesAndReturnsResponse() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Books");

        Category mapped = new Category();
        mapped.setCategoryName("Books");

        Category saved = new Category();
        saved.setCategoryId(10L);
        saved.setCategoryName("Books");

        CategoryResponse response = new CategoryResponse();
        response.setId(10L);

        when(categoryRepository.existsByCategoryNameIgnoreCase("Books")).thenReturn(false);
        when(modelMapper.map(request, Category.class)).thenReturn(mapped);
        when(categoryRepository.save(mapped)).thenReturn(saved);
        when(modelMapper.map(saved, CategoryResponse.class)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertEquals(10L, result.getId());
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
    void updateCategory_whenValid_updatesAndReturnsResponse() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Stationery");

        Category existing = new Category();
        existing.setCategoryId(5L);

        Category saved = new Category();
        saved.setCategoryId(5L);

        CategoryResponse response = new CategoryResponse();
        response.setId(5L);

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByCategoryNameIgnoreCase("Stationery")).thenReturn(Optional.empty());
        doNothing().when(modelMapper).map(request, existing);
        when(categoryRepository.save(existing)).thenReturn(saved);
        when(modelMapper.map(saved, CategoryResponse.class)).thenReturn(response);

        CategoryResponse result = categoryService.updateCategory(5L, request);

        assertEquals(5L, result.getId());
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
    void getAllCategories_returnsMappedList() {
        Category c1 = new Category();
        c1.setCategoryId(1L);
        Category c2 = new Category();
        c2.setCategoryId(2L);

        CategoryResponse r1 = new CategoryResponse();
        r1.setId(1L);
        CategoryResponse r2 = new CategoryResponse();
        r2.setId(2L);

        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(modelMapper.map(c1, CategoryResponse.class)).thenReturn(r1);
        when(modelMapper.map(c2, CategoryResponse.class)).thenReturn(r2);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
    }

    @Test
    void deleteCategory_whenNotFound_throwsCategoryNotFoundException() {
        when(categoryRepository.existsById(11L)).thenReturn(false);

        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(11L));
        verify(categoryRepository, never()).deleteById(11L);
    }

    @Test
    void deleteCategory_whenFound_deletes() {
        when(categoryRepository.existsById(12L)).thenReturn(true);

        categoryService.deleteCategory(12L);

        verify(categoryRepository).deleteById(12L);
    }
}

