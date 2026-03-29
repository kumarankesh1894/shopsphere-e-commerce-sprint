/**
 * This package contains framework and app configuration classes.
 *
 * What it does:
 * - Defines security behavior.
 * - Creates shared beans used across the service.
 * - Configures Redis caching and cache manager policies.
 * - Configures Swagger/OpenAPI metadata.
 *
 * Why it exists:
 * - Central place for setup.
 * - Avoids repeating config logic in business classes.
 *
 * Methods used in this package:
 * - SecurityConfig.userDetailsService()
 * - SecurityConfig.securityFilterChain(...)
 * - ModelMapperConfig.modelMapper()
 * - RedisConfig.cacheManager(...)
 * - RedisConfig.cacheErrorHandler()
 * - RedisConfig.errorHandler()
 * - SwaggerOpenApiConfig.customOpenAPI()
 *
 * New implementation notes:
 * - Redis is used for read cache with TTL-based cache entries.
 * - Cache errors are handled in fail-open style to avoid API downtime.
 */
package com.shopsphere.orderservice.config;
