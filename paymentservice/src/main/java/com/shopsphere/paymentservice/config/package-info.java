/**
 * This package contains configuration classes for payment service.
 *
 * What it does:
 * - Sets security behavior.
 * - Creates shared helper beans.
 * - Defines OpenAPI metadata for Swagger UI.
 *
 * Why it exists:
 * - Centralizes setup code.
 * - Keeps business classes clean.
 *
 * Methods used in this package:
 * - SecurityConfig.userDetailsService()
 * - SecurityConfig.securityFilterChain(...)
 * - ModelMapperConfig.modelMapper()
 * - SwaggerOpenApiConfig.customOpenAPI()
 *
 * New implementation notes:
 * - Swagger UI endpoints are enabled for interactive testing.
 * - Security remains gateway-first with permissive internal service handling.
 */
package com.shopsphere.paymentservice.config;
