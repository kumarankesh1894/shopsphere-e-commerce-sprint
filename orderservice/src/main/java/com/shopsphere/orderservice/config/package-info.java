/**
 * This package contains framework and app configuration classes.
 *
 * What it does:
 * - Defines security behavior.
 * - Creates shared beans used across the service.
 *
 * Why it exists:
 * - Central place for setup.
 * - Avoids repeating config logic in business classes.
 *
 * Methods used in this package:
 * - SecurityConfig.userDetailsService()
 * - SecurityConfig.securityFilterChain(...)
 * - ModelMapperConfig.modelMapper()
 */
package com.shopsphere.orderservice.config;
