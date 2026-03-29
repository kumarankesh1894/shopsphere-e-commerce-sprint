/**
 * This package contains implementation classes for payment logic.
 *
 * What it does:
 * - Implements payment creation, Razorpay order creation, and verification.
 * - Updates order status through internal service calls.
 * - Handles idempotency and payment reuse rules for retries.
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
 *
 * New implementation notes:
 * - Uses reusable HTTP client for Razorpay order API calls.
 * - Includes SLF4J logs for create/verify flow and downstream status updates.
 * - Keeps failure handling explicit for invalid state and gateway errors.
 */
package com.shopsphere.paymentservice.service.Implementation;
