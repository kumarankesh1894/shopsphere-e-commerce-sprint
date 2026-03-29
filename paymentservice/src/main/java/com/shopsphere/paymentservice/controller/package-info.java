/**
 * This package has payment-related API controllers.
 *
 * What it does:
 * - Accepts payment create and payment verify HTTP requests.
 * - Validates incoming DTOs.
 * - Calls service layer and returns responses.
 * - Exposes internal endpoints consumed by order service.
 *
 * Why it exists:
 * - Keeps API handling separate from payment business logic.
 * - Makes endpoint behavior easy to discover.
 *
 * Methods used in this package:
 * - PaymentController.createPayment(...)
 * - PaymentController.verifyPayment(...)
 *
 * New implementation notes:
 * - Swagger annotations (@Tag, @Operation) are added for API testing in Swagger UI.
 * - Controller logs key request markers for payment create and verify flow.
 */
package com.shopsphere.paymentservice.controller;
