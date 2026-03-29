/**
 * This package contains implementation classes for payment logic.
 *
 * What it does:
 * - Implements payment creation, Razorpay order creation, and verification.
 * - Updates order status through internal service calls.
 *
 * Why it exists:
 * - Keeps real business workflow in one clear layer.
 * - Makes payment flow easier to maintain later.
 *
 * Methods used in this package:
 * - PaymentServiceImpl.createPayment(...)
 * - PaymentServiceImpl.verifyPayment(...)
 * - PaymentServiceImpl.syncOrderStatus(...)
 * - PaymentServiceImpl.convertToDto(...)
 * - PaymentServiceImpl.toPaise(...)
 * - PaymentServiceImpl.safeUpdateOrderStatus(...)
 */
package com.shopsphere.paymentservice.service.Implementation;
