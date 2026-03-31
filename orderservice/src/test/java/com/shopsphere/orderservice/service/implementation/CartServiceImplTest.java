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
import static org.mockito.Mockito.never;
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
    void addToCart_whenProductAlreadyInCart_incrementsQuantity() {
        CartItemRequestDto request = CartItemRequestDto.builder()
                .productId(15L)
                .quantity(2)
                .build();

        ProductResponseDto product = ProductResponseDto.builder()
                .productId(15L)
                .productName("Keyboard")
                .price(new BigDecimal("1200.00"))
                .stock(10)
                .build();

        Cart cart = Cart.builder().id(9L).userId(2L).build();
        CartItem existing = CartItem.builder()
                .productId(15L)
                .productName("Keyboard")
                .price(new BigDecimal("1200.00"))
                .quantity(1)
                .cart(cart)
                .build();

        Cart reloaded = Cart.builder().id(9L).userId(2L).items(List.of(existing)).build();

        when(catalogClient.getProductById(15L)).thenReturn(ApiResponse.success(product, "ok"));
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(9L, 15L)).thenReturn(Optional.of(existing));
        when(cartRepository.findById(9L)).thenReturn(Optional.of(reloaded));

        CartResponseDto response = cartService.addToCart(2L, request);

        assertEquals(3, existing.getQuantity());
        assertEquals(1, response.getItems().size());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addToCart_whenCatalogResponseMissing_throwsRuntimeException() {
        CartItemRequestDto request = CartItemRequestDto.builder()
                .productId(99L)
                .quantity(1)
                .build();

        when(catalogClient.getProductById(99L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> cartService.addToCart(2L, request));
        verify(cartRepository, never()).save(any(Cart.class));
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
    void updateItem_whenCartMissing_throwsCartNotFoundException() {
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> cartService.updateItem(2L, 11L, 2));
    }

    @Test
    void updateItem_whenItemMissing_throwsRuntimeException() {
        Cart cart = Cart.builder().id(7L).userId(2L).build();
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(7L, 11L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.updateItem(2L, 11L, 2));
    }

    @Test
    void updateItem_whenCatalogProductMissing_throwsRuntimeException() {
        Cart cart = Cart.builder().id(7L).userId(2L).build();
        CartItem existing = CartItem.builder().productId(11L).quantity(1).cart(cart).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(7L, 11L)).thenReturn(Optional.of(existing));
        when(catalogClient.getProductById(11L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> cartService.updateItem(2L, 11L, 3));
    }

    @Test
    void updateItem_whenRequestedQuantityExceedsStock_throwsRuntimeException() {
        Cart cart = Cart.builder().id(7L).userId(2L).build();
        CartItem existing = CartItem.builder().productId(11L).quantity(1).cart(cart).build();
        ProductResponseDto product = ProductResponseDto.builder().productId(11L).stock(2).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(7L, 11L)).thenReturn(Optional.of(existing));
        when(catalogClient.getProductById(11L)).thenReturn(ApiResponse.success(product, "ok"));

        assertThrows(RuntimeException.class, () -> cartService.updateItem(2L, 11L, 5));
    }

    @Test
    void updateItem_whenValid_updatesQuantityAndReturnsCart() {
        Cart cart = Cart.builder().id(8L).userId(2L).build();
        CartItem existing = CartItem.builder()
                .productId(11L)
                .productName("Headphone")
                .price(new BigDecimal("999.00"))
                .quantity(1)
                .cart(cart)
                .build();
        ProductResponseDto product = ProductResponseDto.builder().productId(11L).stock(10).build();

        Cart reloaded = Cart.builder().id(8L).userId(2L).items(List.of(existing)).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(8L, 11L)).thenReturn(Optional.of(existing));
        when(catalogClient.getProductById(11L)).thenReturn(ApiResponse.success(product, "ok"));
        when(cartRepository.findById(8L)).thenReturn(Optional.of(reloaded));

        CartResponseDto response = cartService.updateItem(2L, 11L, 4);

        assertEquals(4, existing.getQuantity());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("3996.00"), response.getTotalAmount());
        verify(cartItemRepository).save(existing);
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

    @Test
    void removeItem_whenCartMissing_throwsCartNotFoundException() {
        when(cartRepository.findByUserId(66L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> cartService.removeItem(66L, 10L));
    }

    @Test
    void removeItem_whenItemMissing_throwsRuntimeException() {
        Cart cart = Cart.builder().id(71L).userId(2L).build();
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(71L, 10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cartService.removeItem(2L, 10L));
    }

    @Test
    void getCart_whenExistingCartWithItems_returnsComputedTotal() {
        CartItem i1 = CartItem.builder()
                .productId(1L)
                .productName("A")
                .price(new BigDecimal("10.00"))
                .quantity(2)
                .build();
        CartItem i2 = CartItem.builder()
                .productId(2L)
                .productName("B")
                .price(new BigDecimal("5.50"))
                .quantity(3)
                .build();
        Cart cart = Cart.builder().id(90L).userId(2L).items(List.of(i1, i2)).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));

        CartResponseDto response = cartService.getCart(2L);

        assertEquals(2, response.getItems().size());
        assertEquals(new BigDecimal("36.50"), response.getTotalAmount());
    }

    @Test
    void clearCart_whenCartExists_clearsItemsAndSaves() {
        CartItem item = CartItem.builder()
                .productId(3L)
                .quantity(1)
                .price(new BigDecimal("10.00"))
                .build();
        Cart cart = Cart.builder().id(70L).userId(2L).items(new ArrayList<>(List.of(item))).build();

        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));

        cartService.clearCart(2L);

        assertEquals(0, cart.getItems().size());
        verify(cartRepository).save(cart);
    }
}

