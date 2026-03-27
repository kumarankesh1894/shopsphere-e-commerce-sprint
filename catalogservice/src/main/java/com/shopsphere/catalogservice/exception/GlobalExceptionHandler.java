package com.shopsphere.catalogservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //Product Not Found
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<?> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {

        log.warn("Product not found error at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    // Category Not Found
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<?> handleCategoryNotFound(CategoryNotFoundException ex,HttpServletRequest request) {

        log.warn("Category not found error at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                "CATEGORY_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request);
    }

    // Validation Errors (DTO validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation failed at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "VALIDATION_FAILED");
        body.put("message", "Input validation failed");
        body.put("details", errors);
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Illegal Argument (bad request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid request at {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                "INVALID_REQUEST",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request);
    }

    // Fallback (VERY IMPORTANT)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    // Common Response Builder
    private ResponseEntity<?> buildResponse(String errorCode, String message, HttpStatus status, HttpServletRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", errorCode);
        body.put("message", message);
        body.put("status", status.value());
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, status);
    }
}