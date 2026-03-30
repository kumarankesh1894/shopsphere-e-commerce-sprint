package com.shopsphere.authservice.controller;

import com.shopsphere.authservice.dto.AuthResponse;
import com.shopsphere.authservice.dto.LoginRequest;
import com.shopsphere.authservice.dto.SignupRequest;
import com.shopsphere.authservice.dto.UserResponse;
import com.shopsphere.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void signup_returnsCreatedUserResponse() {
        SignupRequest request = new SignupRequest();
        request.setName("Ankesh");
        request.setEmail("ankesh@example.com");
        request.setPassword("Secret@123");

        UserResponse userResponse = new UserResponse();
        userResponse.setId(2L);
        userResponse.setEmail("ankesh@example.com");

        when(authService.signup(request)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = authController.signup(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2L, response.getBody().getId());
        verify(authService).signup(request);
    }

    @Test
    void login_returnsTokenResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ankesh@example.com");
        request.setPassword("Secret@123");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("jwt-token-value");

        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("jwt-token-value", response.getBody().getAccessToken());
        verify(authService).login(request);
    }
}

