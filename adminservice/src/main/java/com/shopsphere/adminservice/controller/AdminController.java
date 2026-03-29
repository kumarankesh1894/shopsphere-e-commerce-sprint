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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin APIs", description = "Dashboard, reports, product admin, and order admin operations")
/*
 * What:
 * Main controller for admin features.
 *
 * Why:
 * Admin needs one place to access dashboard, reports, product management,
 * and order management APIs.
 *
 * How:
 * This controller delegates work to service classes.
 * - Report APIs -> AdminReportService
 * - Product APIs -> AdminCatalogService
 */
public class AdminController {

    private final AdminReportService reportService;
    private final AdminCatalogService catalogService;

    // =============================
    // Admin Dashboard APIs
    // =============================

    /*
     * What:
     * Returns dashboard summary for admin.
     *
     * Why:
     * Admin dashboard needs quick numbers (revenue, order count, status count, top products).
     *
     * How:
     * Calls reportService.getDashboard() and wraps result in ApiResponse.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard", description = "Returns total revenue, order counts, status distribution, and top products")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDashboard(), "Dashboard fetched successfully"));
    }

    // =============================
    // Admin Report APIs
    // =============================

    /*
     * What:
     * Returns only sales report block for a date range.
     *
     * Why:
     * Some admin screens call sales data independently.
     *
     * How:
     * Resolves optional dates and delegates to reportService.getSalesReport(...).
     */
    @GetMapping("/reports/sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get sales report", description = "Returns period-wise sales report and grand total")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getSalesReport(resolvedStart, resolvedEnd),
                "Sales report fetched successfully"
        ));
    }

    /*
     * What:
     * Returns top products report for a date range.
     *
     * Why:
     * Product-performance widgets may call this directly.
     *
     * How:
     * Resolves optional dates and delegates to reportService.getTopProducts(...).
     */
    @GetMapping("/reports/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get product report", description = "Returns top products by units sold and revenue")
    public ResponseEntity<ApiResponse<List<TopProductDto>>> getProductReport(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTopProducts(resolvedStart, resolvedEnd),
                "Product report fetched successfully"
        ));
    }

    /*
     * What:
     * Returns customer activity report for a date range.
     *
     * Why:
     * Customer-performance widgets may call this directly.
     *
     * How:
     * Resolves optional dates and delegates to reportService.getTopCustomers(...).
     */
    @GetMapping("/reports/customers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer report", description = "Returns customer activity summary by orders and spend")
    public ResponseEntity<ApiResponse<List<CustomerActivityDto>>> getCustomerReport(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTopCustomers(resolvedStart, resolvedEnd),
                "Customer report fetched successfully"
        ));
    }

    /*
     * What:
     * Returns combined report data (sales + products + customers).
     *
     * Why:
     * Reports page needs all core report blocks in one API call.
     *
     * How:
     * 1) Resolve date range (default: last 30 days if not provided).
     * 2) Fetch each report block from service.
     * 3) Return combined map in ApiResponse.
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get reports summary", description = "Returns sales, top products, and customer activity for a date range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);

        SalesReportResponse sales = reportService.getSalesReport(resolvedStart, resolvedEnd);
        List<TopProductDto> products = reportService.getTopProducts(resolvedStart, resolvedEnd);
        List<CustomerActivityDto> customers = reportService.getTopCustomers(resolvedStart, resolvedEnd);

        Map<String, Object> data = Map.of(
                "sales", sales,
                "products", products,
                "customers", customers
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Reports fetched successfully"));
    }

    // =============================
    // Admin Order Management APIs
    // =============================

    /*
     * What:
     * Returns all orders for admin management view.
     *
     * Why:
     * Admin order screen needs current order list from source data.
     *
     * How:
     * Calls reportService.getOrders() and returns it.
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders", description = "Returns all orders for admin management")
    public ResponseEntity<ApiResponse<List<OrderAdminDto>>> getOrders() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOrders(), "Orders fetched successfully"));
    }

    /*
     * What:
     * Updates a single order status.
     *
     * Why:
     * Admin can move order lifecycle state from admin panel.
     *
     * How:
     * Delegates status update to reportService, which forwards request to orderservice.
     */
    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Updates order status by delegating to orderservice internal endpoint")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus(
            @PathVariable("id") Long orderId,
            @RequestParam("status") String status
    ) {
        reportService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order status updated"));
    }

    // =============================
    // Admin Product Management APIs
    // =============================

    /*
     * What:
     * Creates a product from admin panel.
     *
     * Why:
     * Admin needs product onboarding API through the same admin service.
     *
     * How:
     * Validates request body and forwards it to catalog service via AdminCatalogService.
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Creates a catalog product through catalogservice")
    public ResponseEntity<ApiResponse<Object>> createProduct(@Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.createProduct(request), "Product created successfully"));
    }

    /*
     * What:
     * Updates existing product details.
     *
     * Why:
     * Admin needs to manage price, stock, and metadata updates.
     *
     * How:
     * Reads product id + request body and delegates to AdminCatalogService.
     */
    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Updates a catalog product through catalogservice")
    public ResponseEntity<ApiResponse<Object>> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateProduct(id, request), "Product updated successfully"));
    }

    /*
     * What:
     * Deletes a product.
     *
     * Why:
     * Admin needs to remove products from catalog.
     *
     * How:
     * Forwards delete request to AdminCatalogService.
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Deletes a catalog product through catalogservice")
    public ResponseEntity<ApiResponse<Object>> deleteProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.deleteProduct(id), "Product deleted successfully"));
    }
}

