/**
 * This package has REST controllers for cart, checkout, and order APIs.
 *
 * What it does:
 * - Receives HTTP requests.
 * - Reads headers like X-UserId and request body data.
 * - Calls service layer methods and returns API responses.
 * - Exposes internal order endpoints for inter-service communication.
 *
 * Why it exists:
 * - Keeps API/web code separate from business logic.
 * - Makes endpoint behavior easy to find.
 * - Separates private user/admin APIs from internal APIs.
 *
 * Methods used in this package:
 * - CartController.addToCart(...)
 * - CartController.getCart(...)
 * - CartController.updateCartItem(...)
 * - CartController.removeItem(...)
 * - CartController.clearCart(...)
 * - CheckoutController.startCheckout(...)
 * - OrderController.getMyOrders(...)
 * - OrderController.getOrder(...)
 * - OrderController.cancelOrder(...)
 * - OrderController.placeOrder(...)
 * - OrderController.shipOrder(...)
 * - OrderController.deliverOrder(...)
 * - OrderController.startPayment(...)
 * - OrderInternalController.updateStatus(...)
 * - OrderInternalController.getOrderForPayment(...)
 *
 * New implementation notes:
 * - Swagger annotations (@Tag, @Operation) are added for API testing in Swagger UI.
 * - Role-based path behavior is handled using gateway-provided headers.
 */
package com.shopsphere.orderservice.controller;
