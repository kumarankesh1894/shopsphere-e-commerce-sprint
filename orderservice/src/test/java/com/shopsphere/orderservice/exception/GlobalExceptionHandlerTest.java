package com.shopsphere.orderservice.exception;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/private/999");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new OrderNotFoundException("missing"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleRetryable_returnsServiceUnavailable() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/private/1/pay");

        RetryableException ex = mock(RetryableException.class);

        ResponseEntity<ApiErrorResponse> response = handler.handleRetryable(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Payment service is temporarily unavailable. Please try again.", response.getBody().getMessage());
    }

    @Test
    void handleFeignException_whenDownstream5xx_mapsToBadGatewayAndExtractsMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/private/1/pay");

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        Response feignResponse = Response.builder()
                .status(500)
                .reason("Server error")
                .request(feignRequest)
                .headers(Map.of())
                .body("{\"message\":\"downstream failed\"}", StandardCharsets.UTF_8)
                .build();
        FeignException ex = FeignException.errorStatus("OrderClient#getOrderById", feignResponse);

        ResponseEntity<ApiErrorResponse> response = handler.handleFeignException(ex, request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("downstream failed", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_returns500() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/private/1");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}

