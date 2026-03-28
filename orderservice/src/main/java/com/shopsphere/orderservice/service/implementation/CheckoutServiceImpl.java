package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.entity.*;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.repository.*;
import com.shopsphere.orderservice.service.CartService;
import com.shopsphere.orderservice.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final CartService cartService;

    @Override
    @Transactional
    public CheckoutResponseDto startCheckout(Long userId, CheckoutRequestDto request) {

        // 1. Idempotency
        Optional<Order> existing =
                orderRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (existing.isPresent()) {
            return new CheckoutResponseDto(existing.get().getId());
        }

        // 2. Fetch cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 3. Convert AddressDto → Address (ModelMapper)
        Address address = modelMapper.map(request.getAddress(), Address.class);

        // 4. Create Order
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CHECKOUT)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .deliveryAddress(address)
                .build();

        // 5. Convert CartItems → OrderItems
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {

            OrderItem orderItem = OrderItem.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .order(order)
                    .build();

            orderItems.add(orderItem);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 6. Set items & total
        order.setItems(orderItems);
        order.setTotalAmount(total);

        // 7. Save (single save with cascade)
        order = orderRepository.save(order);

        // CLEAR CART HERE
        cartService.clearCart(userId);
        return new CheckoutResponseDto(order.getId());
    }
}