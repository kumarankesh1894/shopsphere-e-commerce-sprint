package com.shopsphere.orderservice.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler({OrderNotFoundException.class, CartNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidOrderStateException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({
            OrderAlreadyPackedException.class,
            OrderAlreadyShippedException.class,
            OrderAlreadyDeliveredException.class,
            OrderAlreadyCancelledException.class,
            OrderCancellationNotAllowedException.class,
            OrderTransitionNotAllowedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleOrderLifecycleConflict(RuntimeException ex,
                                                                         HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> details.put(v.getPropertyPath().toString(), v.getMessage()));
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body", request.getRequestURI(), null);
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ApiErrorResponse> handleRetryable(RetryableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment service is temporarily unavailable. Please try again.",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(FeignException ex, HttpServletRequest request) {
        HttpStatus status = mapFeignStatus(ex.status());
        String message = extractDownstreamMessage(ex);
        return buildError(status, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request.getRequestURI(), null);
    }

    private HttpStatus mapFeignStatus(int downstreamStatus) {
        if (downstreamStatus <= 0) {
            return HttpStatus.BAD_GATEWAY;
        }

        HttpStatus resolved = HttpStatus.resolve(downstreamStatus);
        if (resolved == null) {
            return HttpStatus.BAD_GATEWAY;
        }

        // Do not leak raw downstream 5xx; represent as gateway failure.
        if (resolved.is5xxServerError()) {
            return HttpStatus.BAD_GATEWAY;
        }

        return resolved;
    }

    private String extractDownstreamMessage(FeignException ex) {
        String body = ex.contentUTF8();
        if (body == null || body.isBlank()) {
            return "Payment service request failed";
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode messageNode = node.get("message");
            if (messageNode != null && !messageNode.isNull() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }
        } catch (Exception ignored) {
            // Ignore parse errors and use fallback message.
        }

        return "Payment service request failed";
    }

    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status,
                                                        String message,
                                                        String path,
                                                        Map<String, String> details) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
