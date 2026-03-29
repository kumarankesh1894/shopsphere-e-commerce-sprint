package com.shopsphere.adminservice.service.implementation;

import com.shopsphere.adminservice.client.AdminOrderClient;
import com.shopsphere.adminservice.dto.CustomerActivityDto;
import com.shopsphere.adminservice.dto.DashboardResponse;
import com.shopsphere.adminservice.dto.OrderAdminDto;
import com.shopsphere.adminservice.dto.OrderAdminItemDto;
import com.shopsphere.adminservice.dto.SalesDataPointDto;
import com.shopsphere.adminservice.dto.SalesReportResponse;
import com.shopsphere.adminservice.dto.TopProductDto;
import com.shopsphere.adminservice.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
/*
 * What:
 * Implements admin reporting logic.
 *
 * Why:
 * Adminservice receives raw order data and converts it into dashboard/report metrics.
 *
 * How:
 * - Fetch orders from orderservice through Feign.
 * - Aggregate data in memory.
 * - Cache expensive report results in Redis.
 */
public class AdminReportServiceImpl implements AdminReportService {

    private final AdminOrderClient orderClient;

    /*
     * What:
     * Builds dashboard response.
     *
     * Why:
     * Dashboard should show key metrics quickly without recomputing every request.
     *
     * How:
     * 1) Fetch all orders.
     * 2) Calculate revenue/status counts.
     * 3) Calculate top products.
     * 4) Return DashboardResponse (cached).
     */
    @Override
    @Cacheable(value = "dashboard", key = "'summary'")
    public DashboardResponse getDashboard() {
        log.info("admin.dashboard.fetch.start");
        List<OrderAdminDto> orders = orderClient.getAllOrders();

        BigDecimal totalRevenue = orders.stream()
                .filter(this::isRevenueStatus)
                .map(OrderAdminDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statusCounts = orders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(OrderAdminDto::getStatus, Collectors.counting()));

        List<TopProductDto> topProducts = buildTopProducts(orders, 5);

        return DashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders((long) orders.size())
                .statusCounts(statusCounts)
                .topProducts(topProducts)
                .build();
    }

    /*
     * What:
     * Builds sales report for a date range.
     *
     * Why:
     * Reports page needs day-wise totals and a grand total.
     *
     * How:
     * 1) Fetch orders by date range.
     * 2) Keep only revenue statuses.
     * 3) Group by date and sum values.
     * 4) Build SalesReportResponse (cached).
     */
    @Override
    @Cacheable(value = "sales", key = "#start.toString() + '_' + #end.toString()")
    public SalesReportResponse getSalesReport(LocalDate start, LocalDate end) {
        log.info("admin.reports.sales.fetch start={} end={}", start, end);
        List<OrderAdminDto> orders = orderClient.getOrdersByDateRange(start, end).stream()
                .filter(this::isRevenueStatus)
                .toList();

        Map<LocalDate, List<OrderAdminDto>> grouped = orders.stream()
                .filter(o -> o.getPlacedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getPlacedAt().toLocalDate()));

        List<SalesDataPointDto> data = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal dayRevenue = entry.getValue().stream()
                            .map(OrderAdminDto::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return SalesDataPointDto.builder()
                            .period(entry.getKey().toString())
                            .orderCount((long) entry.getValue().size())
                            .revenue(dayRevenue)
                            .build();
                })
                .toList();

        BigDecimal grandTotal = data.stream()
                .map(SalesDataPointDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SalesReportResponse.builder()
                .data(data)
                .grandTotal(grandTotal)
                .build();
    }

    /*
     * What:
     * Returns top products for a date range.
     *
     * Why:
     * Admin needs product performance by units/revenue.
     *
     * How:
     * Fetch orders by range, filter valid revenue statuses,
     * then reuse buildTopProducts helper (cached).
     */
    @Override
    @Cacheable(value = "products", key = "#start.toString() + '_' + #end.toString()")
    public List<TopProductDto> getTopProducts(LocalDate start, LocalDate end) {
        log.info("admin.reports.products.fetch start={} end={}", start, end);
        List<OrderAdminDto> orders = orderClient.getOrdersByDateRange(start, end).stream()
                .filter(this::isRevenueStatus)
                .toList();
        return buildTopProducts(orders, 20);
    }

    /*
     * What:
     * Returns customer activity summary.
     *
     * Why:
     * Admin needs top customers by spend and last activity.
     *
     * How:
     * 1) Fetch and filter orders.
     * 2) Group by userId.
     * 3) Aggregate totals and last order date.
     * 4) Sort by total spend desc (cached).
     */
    @Override
    @Cacheable(value = "users", key = "#start.toString() + '_' + #end.toString()")
    public List<CustomerActivityDto> getTopCustomers(LocalDate start, LocalDate end) {
        log.info("admin.reports.customers.fetch start={} end={}", start, end);
        List<OrderAdminDto> orders = orderClient.getOrdersByDateRange(start, end).stream()
                .filter(this::isRevenueStatus)
                .toList();

        Map<Long, List<OrderAdminDto>> grouped = orders.stream()
                .filter(o -> o.getUserId() != null)
                .collect(Collectors.groupingBy(OrderAdminDto::getUserId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    List<OrderAdminDto> userOrders = entry.getValue();

                    BigDecimal totalSpend = userOrders.stream()
                            .map(OrderAdminDto::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    OrderAdminDto lastOrder = userOrders.stream()
                            .max(Comparator.comparing(OrderAdminDto::getPlacedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);

                    return CustomerActivityDto.builder()
                            .userId(userId)
                            .customerName(lastOrder != null ? lastOrder.getUserName() : null)
                            .totalOrders((long) userOrders.size())
                            .totalSpend(totalSpend)
                            .lastOrderAt(lastOrder != null ? lastOrder.getPlacedAt() : null)
                            .build();
                })
                .sorted(Comparator.comparing(CustomerActivityDto::getTotalSpend, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /*
     * What:
     * Returns complete admin order list.
     *
     * Why:
     * Admin order table needs raw order records with items.
     *
     * How:
     * Calls orderservice internal admin endpoint and caches result.
     */
    @Override
    @Cacheable(value = "orders", key = "'all'")
    public List<OrderAdminDto> getOrders() {
        log.info("admin.orders.fetch.start");
        return orderClient.getAllOrders();
    }

    /*
     * What:
     * Updates order status from admin side.
     *
     * Why:
     * Status changes affect dashboard/reports, so cache must be invalidated.
     *
     * How:
     * 1) Evict all report caches.
     * 2) Forward update request to orderservice.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "sales", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public void updateOrderStatus(Long orderId, String status) {
        log.info("admin.orders.status_update orderId={} status={}", orderId, status);
        orderClient.updateOrderStatus(orderId, status);
    }

    /*
     * What:
     * Checks whether an order status should count in revenue reports.
     *
     * Why:
     * Draft/failed/cancelled states should not inflate revenue numbers.
     *
     * How:
     * Return true only for PAID, SHIPPED, DELIVERED.
     */
    private boolean isRevenueStatus(OrderAdminDto order) {
        if (order.getStatus() == null) {
            return false;
        }
        return "PAID".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus());
    }

    /*
     * What:
     * Aggregates product-level sales from order items.
     *
     * Why:
     * Both dashboard and reports need top products by revenue.
     *
     * How:
     * 1) Iterate each order item.
     * 2) Accumulate units and revenue per product id.
     * 3) Convert to TopProductDto list, sort by revenue desc, apply limit.
     */
    private List<TopProductDto> buildTopProducts(List<OrderAdminDto> orders, int limit) {
        Map<Long, TopProductAccumulator> agg = new HashMap<>();

        for (OrderAdminDto order : orders) {
            if (order.getItems() == null) {
                continue;
            }
            for (OrderAdminItemDto item : order.getItems()) {
                if (item.getProductId() == null) {
                    continue;
                }
                TopProductAccumulator current = agg.computeIfAbsent(item.getProductId(), k -> new TopProductAccumulator());
                current.productName = item.getProductName();
                current.unitsSold += item.getQuantity() == null ? 0 : item.getQuantity();
                BigDecimal lineRevenue = (item.getPrice() == null || item.getQuantity() == null)
                        ? BigDecimal.ZERO
                        : item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                current.revenue = current.revenue.add(lineRevenue);
            }
        }

        return agg.entrySet().stream()
                .map(entry -> TopProductDto.builder()
                        .productId(entry.getKey())
                        .productName(entry.getValue().productName)
                        .unitsSold(entry.getValue().unitsSold)
                        .revenue(entry.getValue().revenue)
                        .build())
                .sorted(Comparator.comparing(TopProductDto::getRevenue, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    /*
     * Small mutable helper used during aggregation.
     * It keeps running totals before final DTO conversion.
     */
    private static class TopProductAccumulator {
        private String productName;
        private long unitsSold;
        private BigDecimal revenue = BigDecimal.ZERO;
    }
}

