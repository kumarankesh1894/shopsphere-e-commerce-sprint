/**
 * This package contains DTOs (request/response models).
 *
 * What it does:
 * - Defines input/output shapes for APIs and inter-service calls.
 * - Carries data between layers without exposing entities.
 * - Defines response models for order history pagination and payment initiation.
 *
 * Why it exists:
 * - Keeps API contract clean and stable.
 * - Separates transport data from DB models.
 *
 * Methods used in this package:
 * - Most DTO methods are Lombok-generated getters/setters/builders.
 * - ApiResponse.success(...)
 * - ApiResponse.failure(...)
 *
 * New implementation notes:
 * - DTOs are used by Swagger-documented endpoints in cart, checkout, and order APIs.
 * - Internal order-payment data exchange uses dedicated lightweight DTOs.
 */
package com.shopsphere.orderservice.dto;

