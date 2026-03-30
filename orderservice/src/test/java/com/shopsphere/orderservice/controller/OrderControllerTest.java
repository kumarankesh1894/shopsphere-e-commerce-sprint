package com.shopsphere.orderservice.controller;

import com.shopsphere.orderservice.dto.ApiResponse;
import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getMyOrders_returnsPaginatedHistory() {
        OrderHistoryPageDto pageDto = OrderHistoryPageDto.builder()
                .orders(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .build();

        when(orderService.getMyOrders(2L, 0, 10, null)).thenReturn(pageDto);

        ResponseEntity<ApiResponse<OrderHistoryPageDto>> response = orderController.getMyOrders(2L, 0, 10, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Order history fetched successfully", response.getBody().getMessage());
        assertEquals(0, response.getBody().getData().getTotalPages());
    }

    @Test
    void startPayment_returnsPaymentInitiatedResponse() {
        PaymentResponseDto payment = new PaymentResponseDto();
        payment.setOrderId(3L);
        payment.setPaymentStatus("PROCESSING");
        payment.setGateway("RAZORPAY");

        when(orderService.startPayment(3L, 2L)).thenReturn(payment);

        ResponseEntity<ApiResponse<PaymentResponseDto>> response = orderController.startPayment(3L, 2L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Payment initiated", response.getBody().getMessage());
        assertEquals("PROCESSING", response.getBody().getData().getPaymentStatus());
    }

    @Test
    void placeOrder_withNonAdminRole_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> orderController.placeOrder(5L, "USER"));
    }

    @Test
    void placeOrder_withAdminRole_delegatesToService() {
        ResponseEntity<ApiResponse<Long>> response = orderController.placeOrder(5L, "ADMIN");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Order packed successfully", response.getBody().getMessage());
        verify(orderService).placeOrder(5L);
    }
}

