package com.shopsphere.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets.*;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    // Secret key for signing and verifying JWTs, picked up from authservice

    @Value("${jwt.secret}")
     private String secret;

    //Build the signing key using the secret key
    private SecretKey getSigningKey(){

        return Keys.hmacShaKeyFor(secret.getBytes()); //HMAC-SHA algorithm for signing the JWT
    }

    //parse JWT once
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //  Clean helper methods (NO re-parsing)
    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public Long extractUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }
}
