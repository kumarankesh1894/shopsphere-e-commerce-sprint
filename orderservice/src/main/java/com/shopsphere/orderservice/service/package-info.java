/**
 * This package defines service interfaces (business contracts).
 *
 * What it does:
 * - Declares cart, checkout, and order operations.
 *
 * Why it exists:
 * - Keeps business contract clear.
 * - Lets controllers depend on interfaces, not implementations.
 *
 * Methods used in this package:
 * - CartService.addToCart(...)
 * - CartService.getCart(...)
 * - CartService.updateItem(...)
 * - CartService.removeItem(...)
 * - CartService.clearCart(...)
 * - CheckoutService.startCheckout(...)
 * - OrderService.getOrder(...)
 * - OrderService.updateOrderStatus(...)
 * - OrderService.placeOrder(...)
 * - OrderService.getOrderById(...)
 * - OrderService.startPayment(...)
 */
package com.shopsphere.orderservice.service;
