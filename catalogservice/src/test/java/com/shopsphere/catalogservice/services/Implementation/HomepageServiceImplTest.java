package com.shopsphere.catalogservice.services.Implementation;

import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.dto.FeaturedProductResponse;
import com.shopsphere.catalogservice.dto.HomepageResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.entities.Product;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private HomepageServiceImpl homepageService;

    @Test
    void getHomepageData_returnsMappedCategoriesAndFeaturedProducts() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");

        Product product = new Product();
        product.setProductId(10L);
        product.setProductName("Phone");
        product.setCategory(category);

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setId(1L);
        categoryResponse.setName("Electronics");

        FeaturedProductResponse featuredResponse = new FeaturedProductResponse();
        featuredResponse.setProductId(10L);
        featuredResponse.setProductName("Phone");

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productRepository.findByFeaturedTrueAndIsAvailableTrue()).thenReturn(List.of(product));
        when(modelMapper.map(category, CategoryResponse.class)).thenReturn(categoryResponse);
        when(modelMapper.map(product, FeaturedProductResponse.class)).thenReturn(featuredResponse);

        HomepageResponse response = homepageService.getHomepageData();

        assertEquals(1, response.getCategories().size());
        assertEquals(1, response.getFeaturedProducts().size());
        assertEquals("Electronics", response.getFeaturedProducts().get(0).getCategoryName());
    }
}

