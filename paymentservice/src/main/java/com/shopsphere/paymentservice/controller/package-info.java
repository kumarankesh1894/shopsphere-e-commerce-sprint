/**
 * This package has payment-related API controllers.
 *
 * What it does:
 * - Accepts payment create and payment verify HTTP requests.
 * - Validates incoming DTOs.
 * - Calls service layer and returns responses.
 *
 * Why it exists:
 * - Keeps API handling separate from payment business logic.
 * - Makes endpoint behavior easy to discover.
 *
 * Methods used in this package:
 * - PaymentController.createPayment(...)
 * - PaymentController.verifyPayment(...)
 */
package com.shopsphere.paymentservice.controller;
