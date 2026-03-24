package com.shopsphere.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component

public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    //Generate JWT
    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(email)                 // email as subject
                .claim("userId", userId)        // userId claim
                .claim("role", role)            //  role claim
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }
}
