package com.shopsphere.catalogservice.controller;

import com.shopsphere.catalogservice.dto.ApiResponse;
import com.shopsphere.catalogservice.dto.ProductRequest;
import com.shopsphere.catalogservice.dto.ProductResponse;
import com.shopsphere.catalogservice.services.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void getProductById_returnsWrappedSuccessResponse() {
        ProductResponse product = new ProductResponse(
                5L,
                "Samsung Galaxy S24",
                "Flagship phone",
                "https://img.example.com/s24.png",
                new BigDecimal("79999.00"),
                20,
                1L,
                true
        );

        when(productService.getProductById(5L)).thenReturn(product);

        ResponseEntity<ApiResponse<ProductResponse>> response = productController.getProductById(5L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Product fetched successfully", response.getBody().getMessage());
        assertEquals("Samsung Galaxy S24", response.getBody().getData().getProductName());
    }

    @Test
    void createProduct_delegatesToService() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Wireless Mouse");
        request.setProductDescription("Bluetooth mouse");
        request.setPrice(new BigDecimal("999.00"));
        request.setStock(35);
        request.setImageUrl("https://img.example.com/mouse.png");
        request.setCategoryId(1L);

        ProductResponse created = new ProductResponse(
                12L,
                "Wireless Mouse",
                "Bluetooth mouse",
                "https://img.example.com/mouse.png",
                new BigDecimal("999.00"),
                35,
                1L,
                true
        );

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(created);

        ResponseEntity<ApiResponse<ProductResponse>> response = productController.createProduct(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(12L, response.getBody().getData().getProductId());
        verify(productService).createProduct(request);
    }
}

