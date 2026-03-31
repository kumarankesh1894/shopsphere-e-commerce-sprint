package com.shopsphere.adminservice.controller;

import com.shopsphere.adminservice.dto.ApiResponse;
import com.shopsphere.adminservice.dto.CustomerActivityDto;
import com.shopsphere.adminservice.dto.DashboardResponse;
import com.shopsphere.adminservice.dto.OrderAdminDto;
import com.shopsphere.adminservice.dto.ProductRequestDto;
import com.shopsphere.adminservice.dto.SalesReportResponse;
import com.shopsphere.adminservice.dto.TopProductDto;
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

    @Test
    void getProductReport_withProvidedDates_delegatesToService() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 30);
        List<TopProductDto> topProducts = List.of(
                TopProductDto.builder().productId(1L).productName("Keyboard").unitsSold(8L).build()
        );

        when(reportService.getTopProducts(start, end)).thenReturn(topProducts);

        ResponseEntity<ApiResponse<List<TopProductDto>>> response = adminController.getProductReport(start, end);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(reportService).getTopProducts(start, end);
    }

    @Test
    void getCustomerReport_withProvidedDates_delegatesToService() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 30);
        List<CustomerActivityDto> customers = List.of(
                CustomerActivityDto.builder().userId(5L).customerName("Riya").totalOrders(3L).build()
        );

        when(reportService.getTopCustomers(start, end)).thenReturn(customers);

        ResponseEntity<ApiResponse<List<CustomerActivityDto>>> response = adminController.getCustomerReport(start, end);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(reportService).getTopCustomers(start, end);
    }

    @Test
    void getReports_whenDatesNull_usesDefaultRangeAndReturnsCombinedData() {
        SalesReportResponse sales = SalesReportResponse.builder().data(List.of()).grandTotal(new BigDecimal("100.00")).build();
        List<TopProductDto> products = List.of(TopProductDto.builder().productId(2L).productName("Mouse").unitsSold(10L).build());
        List<CustomerActivityDto> customers = List.of(CustomerActivityDto.builder().userId(7L).customerName("Aman").totalOrders(4L).build());

        when(reportService.getSalesReport(any(LocalDate.class), any(LocalDate.class))).thenReturn(sales);
        when(reportService.getTopProducts(any(LocalDate.class), any(LocalDate.class))).thenReturn(products);
        when(reportService.getTopCustomers(any(LocalDate.class), any(LocalDate.class))).thenReturn(customers);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = adminController.getReports(null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Reports fetched successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData().get("sales"));
        assertNotNull(response.getBody().getData().get("products"));
        assertNotNull(response.getBody().getData().get("customers"));
        verify(reportService).getSalesReport(any(LocalDate.class), any(LocalDate.class));
        verify(reportService).getTopProducts(any(LocalDate.class), any(LocalDate.class));
        verify(reportService).getTopCustomers(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getOrders_returnsOrdersResponse() {
        List<OrderAdminDto> orders = List.of(
                OrderAdminDto.builder().orderId(21L).userId(9L).status("PAID").build()
        );
        when(reportService.getOrders()).thenReturn(orders);

        ResponseEntity<ApiResponse<List<OrderAdminDto>>> response = adminController.getOrders();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(reportService).getOrders();
    }

    @Test
    void updateOrderStatus_delegatesToReportService() {
        ResponseEntity<ApiResponse<String>> response = adminController.updateOrderStatus(44L, "SHIPPED");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Order status updated", response.getBody().getMessage());
        verify(reportService).updateOrderStatus(44L, "SHIPPED");
    }

    @Test
    void updateProduct_delegatesToCatalogService() {
        ProductRequestDto request = new ProductRequestDto();
        request.setProductName("Mouse V2");
        request.setStock(20);

        when(catalogService.updateProduct(8L, request)).thenReturn(Map.of("updated", true));

        ResponseEntity<ApiResponse<Object>> response = adminController.updateProduct(8L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Product updated successfully", response.getBody().getMessage());
        verify(catalogService).updateProduct(8L, request);
    }

    @Test
    void deleteProduct_delegatesToCatalogService() {
        when(catalogService.deleteProduct(8L)).thenReturn(Map.of("deleted", true));

        ResponseEntity<ApiResponse<Object>> response = adminController.deleteProduct(8L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Product deleted successfully", response.getBody().getMessage());
        verify(catalogService).deleteProduct(8L);
    }
}

