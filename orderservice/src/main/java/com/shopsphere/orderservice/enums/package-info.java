/**
 * This package stores enum constants used by order workflows.
 *
 * What it does:
 * - Defines fixed order states.
 *
 * Why it exists:
 * - Prevents invalid string values.
 * - Makes state checks in methods safe and readable.
 *
 * Methods used with these enums:
 * - OrderService.updateOrderStatus(...)
 * - OrderService.placeOrder(...)
 * - OrderService.startPayment(...)
 * - OrderInternalController.updateStatus(...)
 */
package com.shopsphere.orderservice.enums;
