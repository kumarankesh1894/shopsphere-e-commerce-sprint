/**
 * This package contains DTOs used by payment APIs and inter-service calls.
 *
 * What it does:
 * - Defines request models for create and verify operations.
 * - Defines response model returned to caller.
 * - Carries gateway identifiers, status, and idempotency input.
 *
 * Why it exists:
 * - Separates API models from entity classes.
 * - Keeps contract stable for frontend and other services.
 *
 * Methods used in this package:
 * - DTO methods are mainly Lombok-generated getters/setters.
 *
 * New implementation notes:
 * - DTOs are exposed in Swagger-documented payment endpoints.
 * - Verification DTO is used for signature validation workflow.
 */
package com.shopsphere.paymentservice.dto;

