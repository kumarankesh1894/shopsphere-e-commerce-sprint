/**
 * This package stores enum constants used by payment flow.
 *
 * What it does:
 * - Defines payment state, order state, and gateway type constants.
 * - Provides typed status mapping between payment and order lifecycle.
 *
 * Why it exists:
 * - Prevents invalid string values.
 * - Makes state handling in methods clear and safe.
 *
 * Methods used with these enums:
 * - PaymentServiceImpl.createPayment(...)
 * - PaymentServiceImpl.verifyPayment(...)
 * - PaymentServiceImpl.syncOrderStatus(...)
 * - PaymentServiceImpl.safeUpdateOrderStatus(...)
 *
 * New implementation notes:
 * - Enums are central to success/failed/payment-pending transitions.
 * - Internal status sync calls rely on enum-safe request values.
 */
package com.shopsphere.paymentservice.enums;
