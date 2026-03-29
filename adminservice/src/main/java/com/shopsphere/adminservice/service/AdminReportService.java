package com.shopsphere.adminservice.service;

import com.shopsphere.adminservice.dto.CustomerActivityDto;
import com.shopsphere.adminservice.dto.DashboardResponse;
import com.shopsphere.adminservice.dto.OrderAdminDto;
import com.shopsphere.adminservice.dto.SalesReportResponse;
import com.shopsphere.adminservice.dto.TopProductDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminReportService {
    /*
     * What: Returns dashboard summary.
     * Why: Admin home page needs quick metrics.
     * How: Service aggregates order data and returns DashboardResponse.
     */
    DashboardResponse getDashboard();

    /*
     * What: Returns sales report for date range.
     * Why: Admin reports need period-wise revenue data.
     * How: Service aggregates orders between start and end.
     */
    SalesReportResponse getSalesReport(LocalDate start, LocalDate end);

    /*
     * What: Returns top products report.
     * Why: Admin needs product performance view.
     * How: Service computes units and revenue by product.
     */
    List<TopProductDto> getTopProducts(LocalDate start, LocalDate end);

    /*
     * What: Returns customer activity report.
     * Why: Admin needs top customers by spend.
     * How: Service groups orders by user and aggregates spend/orders.
     */
    List<CustomerActivityDto> getTopCustomers(LocalDate start, LocalDate end);

    /*
     * What: Returns all admin order records.
     * Why: Admin order management screen needs order list.
     * How: Service fetches order data from orderservice.
     */
    List<OrderAdminDto> getOrders();

    /*
     * What: Updates order status.
     * Why: Admin controls lifecycle transitions.
     * How: Service forwards update to orderservice.
     */
    void updateOrderStatus(Long orderId, String status);
}

