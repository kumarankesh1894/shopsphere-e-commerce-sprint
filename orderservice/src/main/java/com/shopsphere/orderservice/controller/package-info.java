/**
 * This package has REST controllers for cart, checkout, and order APIs.
 *
 * What it does:
 * - Receives HTTP requests.
 * - Reads headers like X-UserId and request body data.
 * - Calls service layer methods and returns API responses.
 *
 * Why it exists:
 * - Keeps API/web code separate from business logic.
 * - Makes endpoint behavior easy to find.
 *
 * Methods used in this package:
 * - CartController.addToCart(...)
 * - CartController.getCart(...)
 * - CartController.updateCartItem(...)
 * - CartController.removeItem(...)
 * - CartController.clearCart(...)
 * - CheckoutController.startCheckout(...)
 * - OrderController.getOrder(...)
 * - OrderController.placeOrder(...)
 * - OrderController.startPayment(...)
 * - OrderInternalController.updateStatus(...)
 * - OrderInternalController.getOrderForPayment(...)
 */
package com.shopsphere.orderservice.controller;
