package com.shopsphere.authservice.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private UserResponse userResponse;
}
