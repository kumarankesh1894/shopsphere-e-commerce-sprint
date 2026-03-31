package com.shopsphere.paymentservice.exception;

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
    void handlePaymentVerification_returnsBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/internal/verify");

        ResponseEntity<ApiErrorResponse> response =
                handler.handlePaymentVerification(new PaymentVerificationException("bad sign"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handlePaymentException_returnsBadGateway() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/internal");

        ResponseEntity<ApiErrorResponse> response =
                handler.handlePaymentException(new PaymentException("gateway failed"), request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleRetryable_returnsServiceUnavailable() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/internal");

        RetryableException ex = mock(RetryableException.class);

        ResponseEntity<ApiErrorResponse> response = handler.handleRetryable(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleFeignException_whenBodyHasMessage_usesMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/internal");

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, StandardCharsets.UTF_8, null);
        Response feignResponse = Response.builder()
                .status(404)
                .reason("Not found")
                .request(feignRequest)
                .headers(Map.of())
                .body("{\"message\":\"order not found\"}", StandardCharsets.UTF_8)
                .build();
        FeignException ex = FeignException.errorStatus("OrderClient#getOrderById", feignResponse);

        ResponseEntity<ApiErrorResponse> response = handler.handleFeignException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("order not found", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/internal");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}

