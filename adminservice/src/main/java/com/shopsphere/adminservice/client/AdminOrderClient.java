package com.shopsphere.adminservice.client;

import com.shopsphere.adminservice.dto.OrderAdminDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "orderservice")
/*
 * What:
 * Feign client for calling orderservice internal admin endpoints.
 *
 * Why:
 * Adminservice does not own order data; orderservice is source of truth.
 *
 * How:
 * Each method maps to an internal orderservice endpoint.
 */
public interface AdminOrderClient {

    /*
     * What: Fetch all orders for admin.
     * Why: Needed for dashboard and order management list.
     * How: Calls GET /api/orders/internal/admin.
     */
    @GetMapping("/api/orders/internal/admin")
    List<OrderAdminDto> getAllOrders();

    /*
     * What: Fetch orders inside a date range.
     * Why: Needed for reports calculations.
     * How: Calls GET /api/orders/internal/admin/report with start/end params.
     */
    @GetMapping("/api/orders/internal/admin/report")
    List<OrderAdminDto> getOrdersByDateRange(
            @RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end
    );

    /*
     * What: Update order lifecycle status.
     * Why: Admin can manage fulfillment lifecycle from admin panel.
     * How: Calls PUT /api/orders/internal/{orderId}/status.
     */
    @PutMapping("/api/orders/internal/{orderId}/status")
    void updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("status") String status
    );
}

