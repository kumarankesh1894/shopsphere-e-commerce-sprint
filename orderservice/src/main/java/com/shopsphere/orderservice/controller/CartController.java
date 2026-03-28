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

    // Add item to cart
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

    // Get current cart
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart(
            @RequestHeader("X-UserId") Long userId
    ) {
        CartResponseDto cart = cartService.getCart(userId);

        return ResponseEntity.ok(
                ApiResponse.success(cart, "Cart fetched successfully")
        );
    }

    // Update item quantity
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

    // Remove item from cart

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

        // Clear cart

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