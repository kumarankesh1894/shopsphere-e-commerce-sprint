package com.shopsphere.authservice.service;

import com.shopsphere.authservice.dto.AuthResponse;
import com.shopsphere.authservice.dto.LoginRequest;
import com.shopsphere.authservice.dto.SignupRequest;
import com.shopsphere.authservice.dto.UserResponse;

public interface AuthService {
    UserResponse signup(SignupRequest signupRequest);
    AuthResponse login(LoginRequest loginRequest);
}
