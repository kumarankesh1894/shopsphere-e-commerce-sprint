package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.dto.AddressDto;
import com.shopsphere.orderservice.dto.CheckoutRequestDto;
import com.shopsphere.orderservice.dto.CheckoutResponseDto;
import com.shopsphere.orderservice.entity.Address;
import com.shopsphere.orderservice.entity.Cart;
import com.shopsphere.orderservice.entity.CartItem;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.repository.CartRepository;
import com.shopsphere.orderservice.repository.OrderRepository;
import com.shopsphere.orderservice.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    @Test
    void startCheckout_whenIdempotencyOrderExists_returnsExistingOrderId() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .idempotencyKey("idem-1")
                .build();

        Order existing = Order.builder()
                .id(44L)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        CheckoutResponseDto response = checkoutService.startCheckout(2L, request);

        assertEquals(44L, response.getOrderId());
    }

    @Test
    void startCheckout_whenCartEmpty_throwsRuntimeException() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .idempotencyKey("idem-2")
                .address(new AddressDto())
                .build();

        Cart cart = Cart.builder().id(7L).userId(2L).items(List.of()).build();

        when(orderRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));

        assertThrows(RuntimeException.class, () -> checkoutService.startCheckout(2L, request));
    }

    @Test
    void startCheckout_whenValidCart_createsOrderAndClearsCart() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .idempotencyKey("idem-3")
                .address(AddressDto.builder().fullName("Demo User").build())
                .build();

        Cart cart = Cart.builder().id(9L).userId(2L).build();
        CartItem item1 = CartItem.builder()
                .productId(1L)
                .productName("Phone")
                .price(new BigDecimal("100.00"))
                .quantity(2)
                .cart(cart)
                .build();
        CartItem item2 = CartItem.builder()
                .productId(2L)
                .productName("Cable")
                .price(new BigDecimal("50.00"))
                .quantity(1)
                .cart(cart)
                .build();
        cart.setItems(List.of(item1, item2));

        Address mappedAddress = Address.builder().fullName("Demo User").build();

        Order savedOrder = Order.builder()
                .id(99L)
                .userId(2L)
                .createdAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("250.00"))
                .build();

        when(orderRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.of(cart));
        when(modelMapper.map(request.getAddress(), Address.class)).thenReturn(mappedAddress);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        CheckoutResponseDto response = checkoutService.startCheckout(2L, request);

        assertEquals(99L, response.getOrderId());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(new BigDecimal("250.00"), orderCaptor.getValue().getTotalAmount());
        verify(cartService).clearCart(2L);
    }
}

