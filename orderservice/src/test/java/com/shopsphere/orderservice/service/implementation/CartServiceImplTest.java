package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.CatalogServiceClient;
import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.CartItemRequestDto;
import com.shopsphere.orderservice.dto.CartResponseDto;
import com.shopsphere.orderservice.dto.ProductResponseDto;
import com.shopsphere.orderservice.entity.Cart;
import com.shopsphere.orderservice.entity.CartItem;
import com.shopsphere.orderservice.exception.CartNotFoundException;
import com.shopsphere.orderservice.repository.CartItemRepository;
import com.shopsphere.orderservice.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CatalogServiceClient catalogClient;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void addToCart_whenValidAndCartMissing_createsCartAndReturnsResponse() {
        CartItemRequestDto request = CartItemRequestDto.builder()
                .productId(11L)
                .quantity(2)
                .build();

        ProductResponseDto product = ProductResponseDto.builder()
                .productId(11L)
                .productName("Headphone")
                .price(new BigDecimal("999.00"))
                .stock(10)
                .build();

        Cart createdCart = Cart.builder().id(5L).userId(2L).items(new ArrayList<>()).build();

        Cart reloadedCart = Cart.builder().id(5L).userId(2L).build();
        CartItem item = CartItem.builder()
                .productId(11L)
                .productName("Headphone")
                .price(new BigDecimal("999.00"))
                .quantity(2)
                .cart(reloadedCart)
                .build();
        reloadedCart.setItems(List.of(item));

        when(catalogClient.getProductById(11L)).thenReturn(ApiResponse.success(product, "ok"));
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(createdCart);
        when(cartItemRepository.findByCartIdAndProductId(5L, 11L)).thenReturn(Optional.empty());
        when(cartRepository.findById(5L)).thenReturn(Optional.of(reloadedCart));

        CartResponseDto response = cartService.addToCart(2L, request);

        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("1998.00"), response.getTotalAmount());
    }

    @Test
    void addToCart_whenProductOutOfStock_throwsRuntimeException() {
        CartItemRequestDto request = CartItemRequestDto.builder()
                .productId(12L)
                .quantity(5)
                .build();

        ProductResponseDto product = ProductResponseDto.builder()
                .productId(12L)
                .productName("Mouse")
                .price(new BigDecimal("499.00"))
                .stock(1)
                .build();

        when(catalogClient.getProductById(12L)).thenReturn(ApiResponse.success(product, "ok"));

        assertThrows(RuntimeException.class, () -> cartService.addToCart(2L, request));
    }

    @Test
    void getCart_whenMissing_returnsEmptyCartResponse() {
        when(cartRepository.findByUserId(3L)).thenReturn(Optional.empty());

        CartResponseDto response = cartService.getCart(3L);

        assertEquals(0, response.getItems().size());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    @Test
    void updateItem_whenQuantityInvalid_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> cartService.updateItem(2L, 11L, 0));
    }

    @Test
    void removeItem_whenFound_deletesItemAndReturnsUpdatedCart() {
        Cart cart = Cart.builder().id(7L).userId(2L).build();
        CartItem existing = CartItem.builder()
                .productId(11L)
                .productName("Headphone")
                .price(new BigDecimal("999.00"))
                .quantity(1)
                .cart(cart)
                .build();

        Cart updated = Cart.builder().id(7L).userId(2L).items(List.of()).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(7L, 11L)).thenReturn(Optional.of(existing));
        when(cartRepository.findById(7L)).thenReturn(Optional.of(updated));

        CartResponseDto response = cartService.removeItem(2L, 11L);

        verify(cartItemRepository).delete(existing);
        assertEquals(0, response.getItems().size());
    }

    @Test
    void clearCart_whenCartMissing_throwsCartNotFoundException() {
        when(cartRepository.findByUserId(55L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> cartService.clearCart(55L));
    }
}

