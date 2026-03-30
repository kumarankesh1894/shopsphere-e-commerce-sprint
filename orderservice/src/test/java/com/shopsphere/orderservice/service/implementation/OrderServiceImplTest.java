package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.CatalogServiceClient;
import com.shopsphere.orderservice.client.PaymentClient;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.entity.OrderItem;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.exception.InvalidOrderStateException;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.repository.OrderRepository;
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
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private CatalogServiceClient catalogClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void startPayment_whenOrderNotCheckout_throwsInvalidOrderStateException() {
        Order order = Order.builder()
                .id(10L)
                .userId(2L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("999.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.startPayment(10L, 2L));
    }

    @Test
    void startPayment_whenCheckout_callsPaymentClientAndReturnsResponse() {
        Order order = Order.builder()
                .id(11L)
                .userId(2L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("1499.00"))
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponseDto paymentResponseDto = new PaymentResponseDto();
        paymentResponseDto.setOrderId(11L);
        paymentResponseDto.setPaymentStatus("PROCESSING");

        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(paymentClient.createPayment(any())).thenReturn(paymentResponseDto);

        PaymentResponseDto response = orderService.startPayment(11L, 2L);

        assertEquals(11L, response.getOrderId());
        assertEquals("PROCESSING", response.getPaymentStatus());
        verify(paymentClient).createPayment(any());
    }

    @Test
    void updateOrderStatus_whenPaid_reducesStockAndSavesOrder() {
        OrderItem item = OrderItem.builder()
                .productId(5L)
                .quantity(3)
                .price(new BigDecimal("500.00"))
                .productName("Keyboard")
                .build();

        Order order = Order.builder()
                .id(20L)
                .userId(2L)
                .status(OrderStatus.PAYMENT_PENDING)
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("1500.00"))
                .build();

        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(20L, OrderStatus.PAID);

        verify(catalogClient).reduceStock(5L, 3);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(OrderStatus.PAID, orderCaptor.getValue().getStatus());
    }

    @Test
    void cancelOrder_whenRequestedByDifferentUser_throwsUnauthorizedException() {
        Order order = Order.builder()
                .id(30L)
                .userId(99L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("999.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class, () -> orderService.cancelOrder(30L, 2L));
    }
}

