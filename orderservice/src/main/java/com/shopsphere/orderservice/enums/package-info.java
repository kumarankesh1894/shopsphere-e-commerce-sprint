/**
 * This package stores enum constants used by order workflows.
 *
 * What it does:
 * - Defines fixed order states.
 * - Provides typed lifecycle states for user, admin, and internal order transitions.
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
 *
 * New implementation notes:
 * - Enum values are also used in order history status filters.
 * - Enum-driven transitions are used before cache eviction on write operations.
 */
package com.shopsphere.orderservice.enums;
