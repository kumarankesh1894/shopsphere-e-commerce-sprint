package com.shopsphere.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailExists(EmailAlreadyExistsException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<?> handleInactive(UserInactiveException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // 🔥 Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return buildResponse(msg, HttpStatus.BAD_REQUEST);
    }

    // 🔥 Generic fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : "Unexpected error occurred";

        return buildResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 🔁 Common response builder
    private ResponseEntity<?> buildResponse(String message, HttpStatus status) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "message", message,
                "status", status.value()
        );
        return new ResponseEntity<>(body, status);
    }
}

