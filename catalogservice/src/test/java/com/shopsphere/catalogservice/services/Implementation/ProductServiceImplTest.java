package com.shopsphere.catalogservice.services.Implementation;

import com.shopsphere.catalogservice.dto.PaginationResponse;
import com.shopsphere.catalogservice.dto.ProductFilterRequest;
import com.shopsphere.catalogservice.dto.ProductRequest;
import com.shopsphere.catalogservice.dto.ProductResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.entities.Product;
import com.shopsphere.catalogservice.exception.CategoryNotFoundException;
import com.shopsphere.catalogservice.exception.ProductNotFoundException;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_whenCategoryMissing_throwsCategoryNotFoundException() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(999L);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_whenValidRequest_setsDefaultsAndSaves() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Mechanical Keyboard");
        request.setProductDescription("RGB keyboard");
        request.setPrice(new BigDecimal("2499.00"));
        request.setStock(5);
        request.setImageUrl("https://img");
        request.setCategoryId(1L);

        Category category = new Category();
        category.setCategoryId(1L);

        Product saved = new Product();
        saved.setProductId(10L);
        saved.setProductName("Mechanical Keyboard");
        saved.setFeatured(false);
        saved.setIsAvailable(true);

        ProductResponse mappedResponse = new ProductResponse();
        mappedResponse.setProductId(10L);
        mappedResponse.setProductName("Mechanical Keyboard");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(modelMapper.map(saved, ProductResponse.class)).thenReturn(mappedResponse);

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        assertEquals("Mechanical Keyboard", productCaptor.getValue().getProductName());
        assertTrue(productCaptor.getValue().getIsAvailable());
        assertEquals(Boolean.FALSE, productCaptor.getValue().getFeatured());
        assertEquals(10L, response.getProductId());
    }

    @Test
    void getProductById_whenFound_returnsMappedResponseWithStockAndPrice() {
        Product product = new Product();
        product.setProductId(3L);
        product.setProductName("Mouse");
        product.setStock(6);
        product.setPrice(new BigDecimal("799.00"));

        ProductResponse mapped = new ProductResponse();
        mapped.setProductId(3L);

        when(productRepository.findByProductIdAndIsAvailableTrue(3L)).thenReturn(Optional.of(product));
        when(modelMapper.map(product, ProductResponse.class)).thenReturn(mapped);

        ProductResponse response = productService.getProductById(3L);

        assertEquals(3L, response.getProductId());
        assertEquals(6, response.getStock());
        assertEquals(new BigDecimal("799.00"), response.getPrice());
    }

    @Test
    void updateProduct_whenProductMissing_throwsProductNotFoundException() {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(1L);

        when(productRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(7L, request));
    }

    @Test
    void deleteProduct_whenFound_marksUnavailable() {
        Product product = new Product();
        product.setProductId(8L);
        product.setIsAvailable(true);

        when(productRepository.findById(8L)).thenReturn(Optional.of(product));

        productService.deleteProduct(8L);

        assertEquals(Boolean.FALSE, product.getIsAvailable());
        verify(productRepository).save(product);
    }

    @Test
    void markAsFeatured_whenFound_setsFlagTrue() {
        Product product = new Product();
        product.setProductId(9L);
        product.setFeatured(false);

        Product saved = new Product();
        saved.setProductId(9L);
        saved.setFeatured(true);

        ProductResponse mapped = new ProductResponse();
        mapped.setProductId(9L);

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(saved);
        when(modelMapper.map(saved, ProductResponse.class)).thenReturn(mapped);

        ProductResponse response = productService.markAsFeatured(9L);

        assertEquals(9L, response.getProductId());
        assertEquals(Boolean.TRUE, product.getFeatured());
    }

    @Test
    void reduceStock_whenInsufficientStock_throwsRuntimeException() {
        Product product = new Product();
        product.setProductId(6L);
        product.setStock(1);

        when(productRepository.findById(6L)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () -> productService.reduceStock(6L, 2));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void reduceStock_whenSufficient_updatesStock() {
        Product product = new Product();
        product.setProductId(6L);
        product.setStock(10);

        when(productRepository.findById(6L)).thenReturn(Optional.of(product));

        productService.reduceStock(6L, 4);

        assertEquals(6, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void getAllProducts_whenSortFieldInvalid_defaultsToProductId() {
        Product product = new Product();
        product.setProductId(3L);
        product.setProductName("Mouse");

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 5), 1);
        ProductResponse mapped = new ProductResponse();
        mapped.setProductId(3L);

        when(productRepository.findByIsAvailableTrue(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(product, ProductResponse.class)).thenReturn(mapped);

        PaginationResponse<ProductResponse> response = productService.getAllProducts(0, 5, "badField", "asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByIsAvailableTrue(pageableCaptor.capture());

        assertEquals("productId: ASC", pageableCaptor.getValue().getSort().toString());
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchProducts_whenCalled_returnsPagedResponse() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword("mouse");

        Product product = new Product();
        product.setProductId(15L);

        ProductResponse mapped = new ProductResponse();
        mapped.setProductId(15L);

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(modelMapper.map(product, ProductResponse.class)).thenReturn(mapped);

        PaginationResponse<ProductResponse> response = productService.searchProducts(filter, 0, 10, "productId", "desc");

        assertEquals(1, response.getContent().size());
        assertEquals(15L, response.getContent().get(0).getProductId());
    }
}
