package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.CartItemRequestDto;
import com.shopsphere.orderservice.dto.CartResponseDto;
import com.shopsphere.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart/private")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /*
     * What:
     * Adds one product entry to the current user's cart.
     *
     * Why:
     * Cart must capture item + quantity before checkout can create an order.
     *
     * How:
     * 1) Reads authenticated userId from header.
     * 2) Validates and forwards item payload to cartService.addToCart(...).
     * 3) Returns updated cart snapshot.
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponseDto>> addToCart(
            @RequestHeader("X-UserId") Long userId,
            @RequestBody @Valid CartItemRequestDto request
    ) {
        CartResponseDto cartResponseDto = cartService.addToCart(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(cartResponseDto, "Item added to cart")
        );
    }

    /*
     * What:
     * Fetches the active cart for the logged-in user.
     *
     * Why:
     * Cart page needs latest totals/items after add/update/remove operations.
     *
     * How:
     * 1) Reads userId from X-UserId.
     * 2) Delegates retrieval to cartService.getCart(...).
     * 3) Wraps cart DTO in ApiResponse.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart(
            @RequestHeader("X-UserId") Long userId
    ) {
        CartResponseDto cart = cartService.getCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart fetched successfully")
        );
    }

    /*
     * What:
     * Updates quantity for a specific product already in the user's cart.
     *
     * Why:
     * Users need quantity adjustments without removing/re-adding items.
     *
     * How:
     * 1) Reads userId, productId, and quantity from request.
     * 2) Delegates update validation + persistence to cartService.updateItem(...).
     * 3) Returns refreshed cart state.
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponseDto>> updateCartItem(
            @RequestHeader("X-UserId") Long userId,
            @PathVariable Long productId,
            @RequestParam int quantity
    ) {
        CartResponseDto response = cartService.updateItem(userId, productId, quantity);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Cart updated successfully")
        );
    }

    /*
     * What:
     * Removes one product from the current user's cart.
     *
     * Why:
     * Lets user clean up cart content before checkout.
     *
     * How:
     * 1) Reads userId and productId.
     * 2) Delegates removal to cartService.removeItem(...).
     * 3) Returns updated cart details.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponseDto>> removeItem(
            @RequestHeader("X-UserId") Long userId,
            @PathVariable Long productId
    ) {
        CartResponseDto response = cartService.removeItem(userId, productId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Item removed from cart")
        );
    }

    /*
     * What:
     * Clears all items from the user's cart.
     *
     * Why:
     * Useful when user wants to start a new shopping session quickly.
     *
     * How:
     * 1) Reads authenticated userId.
     * 2) Calls cartService.clearCart(...).
     * 3) Returns success confirmation message.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCart(
            @RequestHeader("X-UserId") Long userId
    ) {
        cartService.clearCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Cart cleared", "Success")
        );
    }
}