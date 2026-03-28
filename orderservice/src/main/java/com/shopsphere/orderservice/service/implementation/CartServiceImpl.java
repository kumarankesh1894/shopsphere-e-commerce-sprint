package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.CatalogServiceClient;
import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.entity.*;
import com.shopsphere.orderservice.exception.CartNotFoundException;
import com.shopsphere.orderservice.repository.CartItemRepository;
import com.shopsphere.orderservice.repository.CartRepository;
import com.shopsphere.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogServiceClient catalogClient;
    private final ModelMapper modelMapper;

    @Override
    public CartResponseDto addToCart(Long userId, CartItemRequestDto request) {

        // 🔥 1. Fetch product from Catalog Service
        ApiResponse<ProductResponseDto> response = catalogClient.getProductById(request.getProductId());
        
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("Product not found");
        }
        
        ProductResponseDto product = response.getData();

        if (product.getStock() == null || product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Product out of stock");
        }

        // 🔥 2. Find or create cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });

        // 🔥 3. Check if product already exists
        Optional<CartItem> existingItem =
                cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .price(product.getPrice())
                    .quantity(request.getQuantity())
                    .cart(cart)
                    .build();

            cartItemRepository.save(item);
        }

        // 🔥 Reload cart to populate items from database
        cart = cartRepository.findById(cart.getId()).orElseThrow(() -> new RuntimeException("Cart not found"));

        // 🔥 4. Return updated cart
        return mapToCartResponse(cart);
    }

    @Override
    public CartResponseDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .items(java.util.Collections.emptyList())
                        .build());

        return mapToCartResponse(cart);
    }

    //updateItem in Cart
    @Override
    public CartResponseDto updateItem(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        // 🔥 ADD THIS BLOCK HERE
        ApiResponse<ProductResponseDto> response =
                catalogClient.getProductById(productId);

        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("Product not found");
        }

        ProductResponseDto product = response.getData();

        if (product.getStock() < quantity) {
            throw new RuntimeException("Not enough stock");
        }

        // 🔥 THEN update
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        cart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return mapToCartResponse(cart);
    }

    //Remove item from cart
    @Override
    public CartResponseDto removeItem(Long userId, Long productId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        cartItemRepository.delete(item);

        cart = cartRepository.findById(cart.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        return mapToCartResponse(cart);
    }

    //Clear the cart
    @Override
    public void clearCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private CartResponseDto mapToCartResponse(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return CartResponseDto.builder()
                    .items(java.util.Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponseDto.builder()
                .items(cart.getItems().stream()
                        .map(item -> CartItemResponseDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .totalAmount(total)
                .build();

    }
}
