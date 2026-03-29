/**
 * This package defines service interfaces (business contracts).
 *
 * What it does:
 * - Declares cart, checkout, and order operations.
 * - Declares internal operations used by other services.
 *
 * Why it exists:
 * - Keeps business contract clear.
 * - Lets controllers depend on interfaces, not implementations.
 * - Keeps controllers and implementations loosely coupled.
 *
 * Methods used in this package:
 * - CartService.addToCart(...)
 * - CartService.getCart(...)
 * - CartService.updateItem(...)
 * - CartService.removeItem(...)
 * - CartService.clearCart(...)
 * - CheckoutService.startCheckout(...)
 * - OrderService.getOrder(...)
 * - OrderService.getMyOrders(...)
 * - OrderService.cancelOrder(...)
 * - OrderService.updateOrderStatus(...)
 * - OrderService.placeOrder(...)
 * - OrderService.shipOrder(...)
 * - OrderService.deliverOrder(...)
 * - OrderService.cancelOrderAsAdmin(...)
 * - OrderService.getOrderById(...)
 * - OrderService.startPayment(...)
 *
 * New implementation notes:
 * - Read APIs are cache-enabled at implementation level.
 * - Checkout and lifecycle write operations evict related caches.
 * - Payment initiation is part of order lifecycle contract.
 */
package com.shopsphere.orderservice.service;
