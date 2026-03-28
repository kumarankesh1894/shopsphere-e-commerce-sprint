package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.dto.CartItemRequestDto;
import com.shopsphere.orderservice.dto.CartResponseDto;

public interface CartService {

    CartResponseDto addToCart(Long userId, CartItemRequestDto request);
    CartResponseDto getCart(Long userId);
    CartResponseDto updateItem(Long userId, Long productId, int quantity);

    CartResponseDto removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}