package com.shopsphere.apigateway.filter;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    @Test
    void filter_whenPublicPath_bypassesAuth() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build()
        );

        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            called.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(1));

        assertTrue(called.get());
    }

    @Test
    void filter_whenMissingAuthorization_returnsUnauthorized() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/private/1").build()
        );

        filter.filter(exchange, ex -> Mono.empty()).block(Duration.ofSeconds(1));

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_whenInvalidHeaderPrefix_returnsUnauthorized() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/private/1")
                        .header(HttpHeaders.AUTHORIZATION, "Token abc")
                        .build()
        );

        filter.filter(exchange, ex -> Mono.empty()).block(Duration.ofSeconds(1));

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_whenJwtParsingFails_returnsUnauthorized() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.getClaims("bad-token")).thenThrow(new RuntimeException("bad token"));

        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/private/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build()
        );

        filter.filter(exchange, ex -> Mono.empty()).block(Duration.ofSeconds(1));

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_whenAdminRouteAndUserRole_returnsForbidden() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("t1")).thenReturn(claims);
        when(jwtUtil.extractEmail(claims)).thenReturn("u@x.com");
        when(jwtUtil.extractRole(claims)).thenReturn("USER");
        when(jwtUtil.extractUserId(claims)).thenReturn(1L);

        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer t1")
                        .build()
        );

        filter.filter(exchange, ex -> Mono.empty()).block(Duration.ofSeconds(1));

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_whenValidToken_addsForwardHeadersAndContinues() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(jwtUtil.getClaims("ok-token")).thenReturn(claims);
        when(jwtUtil.extractEmail(claims)).thenReturn("admin@x.com");
        when(jwtUtil.extractRole(claims)).thenReturn("ADMIN");
        when(jwtUtil.extractUserId(claims)).thenReturn(77L);

        AuthFilter filter = new AuthFilter(jwtUtil);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/private/1/place")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ok-token")
                        .build()
        );

        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            called.set(true);
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(1));

        assertTrue(called.get());
        assertEquals("admin@x.com", captured.get().getRequest().getHeaders().getFirst("X-Email"));
        assertEquals("ADMIN", captured.get().getRequest().getHeaders().getFirst("X-Role"));
        assertEquals("77", captured.get().getRequest().getHeaders().getFirst("X-UserId"));
    }
}

