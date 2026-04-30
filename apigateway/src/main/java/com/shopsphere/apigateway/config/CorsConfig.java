package com.shopsphere.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * What:
 * Global CORS configuration for the API Gateway.
 *
 * Why:
 * All frontend requests pass through the gateway, so configuring CORS here
 * covers every downstream service without touching each one individually.
 * This replaces the Vite dev proxy — the frontend can call the gateway
 * directly at http://localhost:8080 from any origin.
 *
 * How:
 * CorsWebFilter is the reactive (WebFlux) equivalent of Spring MVC's
 * CorsFilter. It runs before the AuthFilter so preflight OPTIONS requests
 * are answered immediately without hitting JWT validation.
 * (Jab frontend (React) backend ko request bhejne wala hota hai,
 * toh kabhi-kabhi browser pehle ek test request bhejta hai.Is test request ko bolte hain: OPTIONS request (Preflight request))
 * Kyun bhejta hai OPTIONS request?
 * Browser check karta hai:
 *"Kya ye server mujhe allow karega ya block karega?"
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins — add your production frontend URL here when deploying
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000"    // fallback / alternative dev port
        ));

        // Standard HTTP methods used by the frontend
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers the frontend is allowed to send
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header so the frontend can read it if needed
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies / Authorization header to be sent with requests
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS round-trips)
        config.setMaxAge(3600L);

        //use for mapping CORS config to URL patterns(Map (kahan rule apply hoga))
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Har endpoint pe ye CORS rules apply karo

        return new CorsWebFilter(source);
        //(Ab filter ko: rules mil gaye, routes mapping mil gayi, Ab wo kaam kar sakta hai)
    }
}
