/**
 * This package defines payment business interfaces.
 *
 * What it does:
 * - Declares create and verify operations for payment flow.
 * - Defines service contract used by internal payment controller.
 *
 * Why it exists:
 * - Keeps business contract clear for controller and implementation.
 * - Supports clean separation of API layer and gateway integration logic.
 *
 * Methods used in this package:
 * - PaymentService.createPayment(...)
 * - PaymentService.verifyPayment(...)
 *
 * New implementation notes:
 * - Contract supports idempotent payment creation flow.
 * - Verification contract drives order status sync in downstream service.
 */
package com.shopsphere.paymentservice.service;
