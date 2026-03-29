package com.shopsphere.apigateway.filter;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered{
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        //Skip the endpoints
        if(isPublic(path) ){ // isPublic method created below where i had put all the public endpoints
            return chain.filter(exchange); //allow endpoints to pass through without token validation
        }

        // step 1: Check if Authorization header is present
        if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
            return onError(exchange,"Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        // step 2: Extract the token from the header look like: "Bearer <token>"
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        //step 3: Extract claims and forward user info in headers to downstream services

        try {
            //  Parse ONCE
            Claims claims = jwtUtil.getClaims(token);

            String email = jwtUtil.extractEmail(claims);  //extract email
            String role = jwtUtil.extractRole(claims);
            Long userId = jwtUtil.extractUserId(claims);

            // step 4: Role-Based Authorization

            boolean isAdminOrderLifecycleRoute = path.matches("^/api/orders/private/\\d+/(place|ship|deliver)$");

            boolean isAdminRoute = path.startsWith("/api/admin")
                    || path.startsWith("/api/catalog/private")
                    || isAdminOrderLifecycleRoute;

            boolean isUserRoute = path.startsWith("/api/user");

            if (isAdminRoute && !role.equals("ADMIN")) {
                return onError(exchange, "Forbidden: Admins only", HttpStatus.FORBIDDEN);
            }

            if (isUserRoute && !(role.equals("USER") || role.equals("ADMIN"))) {
                return onError(exchange, "Forbidden: Users only", HttpStatus.FORBIDDEN);
            }


            //  Forward headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Email", email)
                    .header("X-Role", role)
                    .header("X-UserId", String.valueOf(userId))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            return onError(exchange, "Invalid or Expired Token", HttpStatus.UNAUTHORIZED);
        }
    }

    //Helper method to handle errors and return appropriate response
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\": \"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Boolean isPublic(String path){
        return path.startsWith("/api/auth/signup")
                ||path.startsWith("/api/auth/login")
                || path.contains("/public");

    }
    @Override
    public int getOrder() {
        return -1; // -1 set high precedence for this filter to run before other filters
    }
}
