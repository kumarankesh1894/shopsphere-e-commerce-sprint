package com.shopsphere.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {

    @Test
    void getClaims_and_extractors_returnExpectedValues() {
        String secret = "this-is-a-very-strong-jwt-secret-for-tests-123456";

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);

        String token = Jwts.builder()
                .subject("user@example.com")
                .claim("role", "ADMIN")
                .claim("userId", 7L)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Claims claims = jwtUtil.getClaims(token);

        assertEquals("user@example.com", jwtUtil.extractEmail(claims));
        assertEquals("ADMIN", jwtUtil.extractRole(claims));
        assertEquals(7L, jwtUtil.extractUserId(claims));
    }
}

