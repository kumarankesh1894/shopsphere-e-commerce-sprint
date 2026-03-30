package com.shopsphere.adminservice.service.implementation;

import com.shopsphere.adminservice.client.AdminCatalogClient;
import com.shopsphere.adminservice.dto.ProductRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceImplTest {

    @Mock
    private AdminCatalogClient catalogClient;

    @InjectMocks
    private AdminCatalogServiceImpl adminCatalogService;

    @Test
    void createProduct_delegatesToCatalogClient() {
        ProductRequestDto request = new ProductRequestDto();
        request.setProductName("Phone");

        when(catalogClient.createProduct(request)).thenReturn("created");

        Object response = adminCatalogService.createProduct(request);

        assertEquals("created", response);
        verify(catalogClient).createProduct(request);
    }

    @Test
    void updateProduct_delegatesToCatalogClient() {
        ProductRequestDto request = new ProductRequestDto();
        request.setProductName("Updated Phone");

        when(catalogClient.updateProduct(7L, request)).thenReturn("updated");

        Object response = adminCatalogService.updateProduct(7L, request);

        assertEquals("updated", response);
        verify(catalogClient).updateProduct(7L, request);
    }

    @Test
    void deleteProduct_delegatesToCatalogClient() {
        when(catalogClient.deleteProduct(9L)).thenReturn("deleted");

        Object response = adminCatalogService.deleteProduct(9L);

        assertEquals("deleted", response);
        verify(catalogClient).deleteProduct(9L);
    }
}

