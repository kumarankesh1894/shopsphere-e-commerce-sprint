/**
 * This package has actual implementations of business logic.
 *
 * What it does:
 * - Implements service interface methods.
 * - Talks to repositories and other services.
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
 * - OrderServiceImpl.updateOrderStatus(...)
 * - OrderServiceImpl.placeOrder(...)
 * - OrderServiceImpl.getOrderById(...)
 * - OrderServiceImpl.startPayment(...)
 * - OrderServiceImpl.buildPaymentRequest(...)
 */
package com.shopsphere.orderservice.service.implementation;
