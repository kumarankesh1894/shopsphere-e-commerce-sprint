package com.shopsphere.adminservice.service.implementation;

import com.shopsphere.adminservice.client.AdminOrderClient;
import com.shopsphere.adminservice.dto.CustomerActivityDto;
import com.shopsphere.adminservice.dto.DashboardResponse;
import com.shopsphere.adminservice.dto.OrderAdminDto;
import com.shopsphere.adminservice.dto.OrderAdminItemDto;
import com.shopsphere.adminservice.dto.SalesReportResponse;
import com.shopsphere.adminservice.dto.TopProductDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceImplTest {

    @Mock
    private AdminOrderClient orderClient;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    @Test
    void getDashboard_calculatesRevenueStatusCountsAndTopProducts() {
        OrderAdminDto paidOrder = buildOrder(1L, 101L, "Alice", "PAID", "2026-03-01T10:00:00",
                new BigDecimal("100.00"),
                List.of(buildItem(11L, "Phone", 1, new BigDecimal("100.00"))));

        OrderAdminDto deliveredOrder = buildOrder(2L, 102L, "Bob", "DELIVERED", "2026-03-02T11:00:00",
                new BigDecimal("250.00"),
                List.of(buildItem(12L, "Keyboard", 2, new BigDecimal("125.00"))));

        OrderAdminDto cancelledOrder = buildOrder(3L, 103L, "Carol", "CANCELLED", "2026-03-02T12:00:00",
                new BigDecimal("999.00"),
                List.of(buildItem(13L, "Mouse", 1, new BigDecimal("999.00"))));

        when(orderClient.getAllOrders()).thenReturn(List.of(paidOrder, deliveredOrder, cancelledOrder));

        DashboardResponse response = adminReportService.getDashboard();

        assertEquals(new BigDecimal("350.00"), response.getTotalRevenue());
        assertEquals(3L, response.getTotalOrders());
        assertEquals(1L, response.getStatusCounts().get("PAID"));
        assertEquals(1L, response.getStatusCounts().get("DELIVERED"));
        assertEquals(1L, response.getStatusCounts().get("CANCELLED"));
        assertNotNull(response.getTopProducts());
        assertEquals("Mouse", response.getTopProducts().get(0).getProductName());
        assertEquals(new BigDecimal("999.00"), response.getTopProducts().get(0).getRevenue());
    }

    @Test
    void getSalesReport_groupsByDayAndReturnsGrandTotal() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 3);

        OrderAdminDto dayOne = buildOrder(10L, 1001L, "Alice", "PAID", "2026-03-01T09:30:00",
                new BigDecimal("120.00"), List.of());
        OrderAdminDto dayTwoFirst = buildOrder(11L, 1002L, "Bob", "SHIPPED", "2026-03-02T13:00:00",
                new BigDecimal("80.00"), List.of());
        OrderAdminDto dayTwoSecond = buildOrder(12L, 1003L, "Chris", "DELIVERED", "2026-03-02T18:45:00",
                new BigDecimal("50.00"), List.of());
        OrderAdminDto cancelled = buildOrder(13L, 1004L, "Donna", "CANCELLED", "2026-03-02T21:00:00",
                new BigDecimal("1000.00"), List.of());

        when(orderClient.getOrdersByDateRange(start, end)).thenReturn(List.of(dayOne, dayTwoFirst, dayTwoSecond, cancelled));

        SalesReportResponse response = adminReportService.getSalesReport(start, end);

        assertEquals(2, response.getData().size());
        assertEquals("2026-03-01", response.getData().get(0).getPeriod());
        assertEquals(new BigDecimal("120.00"), response.getData().get(0).getRevenue());
        assertEquals("2026-03-02", response.getData().get(1).getPeriod());
        assertEquals(2L, response.getData().get(1).getOrderCount());
        assertEquals(new BigDecimal("130.00"), response.getData().get(1).getRevenue());
        assertEquals(new BigDecimal("250.00"), response.getGrandTotal());
    }

    @Test
    void getTopCustomers_returnsSortedByTotalSpend() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        OrderAdminDto aliceOne = buildOrder(20L, 501L, "Alice", "PAID", "2026-03-05T10:00:00",
                new BigDecimal("100.00"), List.of());
        OrderAdminDto aliceTwo = buildOrder(21L, 501L, "Alice", "DELIVERED", "2026-03-10T10:00:00",
                new BigDecimal("150.00"), List.of());
        OrderAdminDto bobOne = buildOrder(22L, 502L, "Bob", "SHIPPED", "2026-03-11T10:00:00",
                new BigDecimal("120.00"), List.of());

        when(orderClient.getOrdersByDateRange(start, end)).thenReturn(List.of(aliceOne, aliceTwo, bobOne));

        List<CustomerActivityDto> response = adminReportService.getTopCustomers(start, end);

        assertEquals(2, response.size());
        assertEquals(501L, response.get(0).getUserId());
        assertEquals(new BigDecimal("250.00"), response.get(0).getTotalSpend());
        assertEquals(2L, response.get(0).getTotalOrders());
        assertEquals(LocalDateTime.parse("2026-03-10T10:00:00"), response.get(0).getLastOrderAt());
    }

    @Test
    void updateOrderStatus_delegatesToOrderClient() {
        adminReportService.updateOrderStatus(44L, "PACKED");

        verify(orderClient).updateOrderStatus(44L, "PACKED");
    }

    @Test
    void getTopProducts_returnsRevenueDescending() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        OrderAdminDto order = buildOrder(30L, 801L, "Eve", "PAID", "2026-03-12T10:00:00",
                new BigDecimal("300.00"),
                List.of(
                        buildItem(1L, "Laptop", 1, new BigDecimal("200.00")),
                        buildItem(2L, "Headphone", 2, new BigDecimal("50.00"))
                ));

        when(orderClient.getOrdersByDateRange(start, end)).thenReturn(List.of(order));

        List<TopProductDto> response = adminReportService.getTopProducts(start, end);

        assertEquals(2, response.size());
        assertEquals("Laptop", response.get(0).getProductName());
        assertEquals(new BigDecimal("200.00"), response.get(0).getRevenue());
        assertEquals("Headphone", response.get(1).getProductName());
        assertEquals(new BigDecimal("100.00"), response.get(1).getRevenue());
    }

    @Test
    void getOrders_delegatesToOrderClient() {
        OrderAdminDto order = buildOrder(99L, 1L, "Demo", "PAID", "2026-03-20T10:00:00",
                new BigDecimal("500.00"), List.of());

        when(orderClient.getAllOrders()).thenReturn(List.of(order));

        List<OrderAdminDto> response = adminReportService.getOrders();

        assertEquals(1, response.size());
        assertEquals(99L, response.get(0).getOrderId());
        verify(orderClient).getAllOrders();
    }

    private OrderAdminDto buildOrder(Long orderId,
                                     Long userId,
                                     String userName,
                                     String status,
                                     String placedAt,
                                     BigDecimal totalAmount,
                                     List<OrderAdminItemDto> items) {
        return OrderAdminDto.builder()
                .orderId(orderId)
                .userId(userId)
                .userName(userName)
                .status(status)
                .placedAt(LocalDateTime.parse(placedAt))
                .totalAmount(totalAmount)
                .items(items)
                .build();
    }

    private OrderAdminItemDto buildItem(Long productId, String productName, Integer quantity, BigDecimal price) {
        return OrderAdminItemDto.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }
}
