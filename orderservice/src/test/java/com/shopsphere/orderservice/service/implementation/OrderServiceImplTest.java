package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.CatalogServiceClient;
import com.shopsphere.orderservice.client.PaymentClient;
import com.shopsphere.orderservice.dto.OrderAdminDto;
import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.entity.OrderItem;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.exception.InvalidOrderStateException;
import com.shopsphere.orderservice.exception.OrderNotFoundException;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void getOrder_whenOwnerMatches_returnsResponse() {
        Order order = Order.builder()
                .id(100L)
                .userId(7L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("300.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.getOrder(100L, 7L);

        assertEquals(100L, response.getOrderId());
        assertEquals(OrderStatus.CHECKOUT, response.getStatus());
    }

    @Test
    void getOrder_whenNotOwner_throwsUnauthorizedException() {
        Order order = Order.builder()
                .id(101L)
                .userId(9L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("300.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class, () -> orderService.getOrder(101L, 2L));
    }

    @Test
    void getMyOrders_whenInvalidStatusFilter_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> orderService.getMyOrders(2L, 0, 10, "NOT_A_STATUS"));
    }

    @Test
    void getMyOrders_whenStatusNotProvided_returnsPagedHistory() {
        Order order = Order.builder()
                .id(102L)
                .userId(2L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("499.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findByUserId(org.mockito.ArgumentMatchers.eq(2L), any(Pageable.class)))
                .thenReturn(page);

        OrderHistoryPageDto response = orderService.getMyOrders(2L, 0, 10, null);

        assertEquals(1, response.getOrders().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void placeOrder_whenPaid_setsPacked() {
        Order order = Order.builder()
                .id(200L)
                .userId(2L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        orderService.placeOrder(200L);

        assertEquals(OrderStatus.PACKED, order.getStatus());
        assertNotNull(order.getPackedAt());
        verify(orderRepository).save(order);
    }

    @Test
    void shipOrder_whenPacked_setsShipped() {
        Order order = Order.builder()
                .id(201L)
                .userId(2L)
                .status(OrderStatus.PACKED)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(201L)).thenReturn(Optional.of(order));

        orderService.shipOrder(201L);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertNotNull(order.getShippedAt());
        verify(orderRepository).save(order);
    }

    @Test
    void deliverOrder_whenShipped_setsDelivered() {
        Order order = Order.builder()
                .id(202L)
                .userId(2L)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(202L)).thenReturn(Optional.of(order));

        orderService.deliverOrder(202L);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertNotNull(order.getDeliveredAt());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderAsAdmin_whenAllowed_setsCancelled() {
        Order order = Order.builder()
                .id(203L)
                .userId(2L)
                .status(OrderStatus.PACKED)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(203L)).thenReturn(Optional.of(order));

        orderService.cancelOrderAsAdmin(203L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getCancelledAt());
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_whenExists_returnsEntity() {
        Order order = Order.builder()
                .id(204L)
                .userId(2L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("199.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(204L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(204L);

        assertEquals(204L, result.getId());
    }

    @Test
    void getOrderById_whenMissing_throwsOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void getAllOrdersForAdmin_returnsMappedList() {
        Order order = Order.builder()
                .id(300L)
                .userId(5L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<OrderAdminDto> result = orderService.getAllOrdersForAdmin();

        assertEquals(1, result.size());
        assertEquals(300L, result.get(0).getOrderId());
    }

    @Test
    void getOrdersByDateRange_returnsMappedList() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 2);

        Order order = Order.builder()
                .id(301L)
                .userId(5L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        when(orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(order));

        List<OrderAdminDto> result = orderService.getOrdersByDateRange(start, end);

        assertEquals(1, result.size());
        assertEquals(301L, result.get(0).getOrderId());
    }
}
