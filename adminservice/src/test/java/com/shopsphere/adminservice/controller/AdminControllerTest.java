package com.shopsphere.adminservice.controller;

import com.shopsphere.adminservice.dto.ApiResponse;
import com.shopsphere.adminservice.dto.DashboardResponse;
import com.shopsphere.adminservice.dto.ProductRequestDto;
import com.shopsphere.adminservice.dto.SalesReportResponse;
import com.shopsphere.adminservice.service.AdminCatalogService;
import com.shopsphere.adminservice.service.AdminReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminReportService reportService;

    @Mock
    private AdminCatalogService catalogService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getDashboard_returnsSuccessResponse() {
        DashboardResponse dashboard = DashboardResponse.builder()
                .totalRevenue(new BigDecimal("12500.00"))
                .totalOrders(12L)
                .statusCounts(Map.of("PAID", 9L))
                .build();
        when(reportService.getDashboard()).thenReturn(dashboard);

        ResponseEntity<ApiResponse<DashboardResponse>> response = adminController.getDashboard();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Dashboard fetched successfully", response.getBody().getMessage());
        assertEquals(12L, response.getBody().getData().getTotalOrders());
    }

    @Test
    void getSalesReport_withProvidedDates_delegatesToService() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 30);
        SalesReportResponse report = SalesReportResponse.builder()
                .data(List.of())
                .grandTotal(new BigDecimal("5000.00"))
                .build();

        when(reportService.getSalesReport(start, end)).thenReturn(report);

        ResponseEntity<ApiResponse<SalesReportResponse>> response = adminController.getSalesReport(start, end);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new BigDecimal("5000.00"), response.getBody().getData().getGrandTotal());
        verify(reportService).getSalesReport(start, end);
    }

    @Test
    void createProduct_delegatesToCatalogService() {
        ProductRequestDto request = new ProductRequestDto();
        request.setProductName("Keyboard");
        request.setPrice(new BigDecimal("1499.00"));
        request.setStock(10);
        request.setImageUrl("https://img.example.com/kb.png");
        request.setCategoryId(1L);

        when(catalogService.createProduct(any(ProductRequestDto.class))).thenReturn(Map.of("id", 11L));

        ResponseEntity<ApiResponse<Object>> response = adminController.createProduct(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Product created successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        verify(catalogService).createProduct(eq(request));
    }
}

