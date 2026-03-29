/**
 * This package stores enum constants used by payment flow.
 *
 * What it does:
 * - Defines payment state, order state, and gateway type constants.
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
 */
package com.shopsphere.paymentservice.enums;
