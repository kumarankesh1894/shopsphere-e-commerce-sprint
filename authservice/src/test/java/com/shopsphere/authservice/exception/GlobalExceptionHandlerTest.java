package com.shopsphere.authservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUserNotFound_returnsNotFound() {
        ResponseEntity<?> response = handler.handleUserNotFound(new UserNotFoundException("user missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleEmailExists_returnsConflict() {
        ResponseEntity<?> response = handler.handleEmailExists(new EmailAlreadyExistsException("exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorized() {
        ResponseEntity<?> response = handler.handleInvalidCredentials(new InvalidCredentialsException("bad creds"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleInactive_returnsForbidden() {
        ResponseEntity<?> response = handler.handleInactive(new UserInactiveException("inactive"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleValidation_returnsFirstFieldMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "email", "invalid email"));

        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class),
                0
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<?> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("invalid email", body.get("message"));
    }

    @Test
    void handleGeneral_withNullMessage_returnsFallback() {
        ResponseEntity<?> response = handler.handleGeneral(new RuntimeException());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Unexpected error occurred", body.get("message"));
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
        // helper signature for MethodParameter creation in test
    }
}

