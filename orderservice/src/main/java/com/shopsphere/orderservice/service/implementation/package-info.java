/**
 * This package has actual implementations of business logic.
 *
 * What it does:
 * - Implements service interface methods.
 * - Talks to repositories and other services.
 * - Applies lifecycle rules, idempotency checks, and status transitions.
 *
 * Why it exists:
 * - Keeps full business workflow in one layer.
 * - Makes maintenance and debugging easier.
 *
 * Methods used in this package:
 * - CartServiceImpl.addToCart(...)
 * - CartServiceImpl.getCart(...)
 * - CartServiceImpl.updateItem(...)
 * - CartServiceImpl.removeItem(...)
 * - CartServiceImpl.clearCart(...)
 * - CartServiceImpl.mapToCartResponse(...)
 * - CheckoutServiceImpl.startCheckout(...)
 * - OrderServiceImpl.getOrder(...)
 * - OrderServiceImpl.getMyOrders(...)
 * - OrderServiceImpl.cancelOrder(...)
 * - OrderServiceImpl.cancelOrderAsAdmin(...)
 * - OrderServiceImpl.updateOrderStatus(...)
 * - OrderServiceImpl.placeOrder(...)
 * - OrderServiceImpl.shipOrder(...)
 * - OrderServiceImpl.deliverOrder(...)
 * - OrderServiceImpl.getOrderById(...)
 * - OrderServiceImpl.startPayment(...)
 * - OrderServiceImpl.buildPaymentRequest(...)
 *
 * New implementation notes:
 * - Read methods use Redis cache; write methods evict related caches.
 * - Cache behavior and lifecycle steps are logged using SLF4J.
 * - Internal order fetch for payment flow is handled safely without fragile entity caching.
 */
package com.shopsphere.orderservice.service.implementation;
