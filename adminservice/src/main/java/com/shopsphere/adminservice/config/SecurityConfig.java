package com.shopsphere.adminservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
/*
 * What:
 * Security setup for adminservice.
 *
 * Why:
 * Admin endpoints use @PreAuthorize, so we need method-security support.
 *
 * How:
 * - Keep service stateless.
 * - Allow Swagger endpoints.
 * - Build authentication from gateway role header.
 */
public class SecurityConfig {

    /*
     * What:
     * Creates main HTTP security filter chain.
     *
     * Why:
     * Defines how requests are filtered before hitting controllers.
     *
     * How:
     * 1) Disable CSRF/form login for API style.
     * 2) Keep session stateless.
     * 3) Allow swagger paths.
     * 4) Add roleHeaderAuthFilter before username/password filter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(roleHeaderAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * What:
     * Creates filter that reads role from request header.
     *
     * Why:
     * Gateway already validates token and forwards role information.
     *
     * How:
     * If X-Role exists, convert it to ROLE_* authority and set authentication
     * in SecurityContext so @PreAuthorize checks can work.
     */
    @Bean
    public OncePerRequestFilter roleHeaderAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request,
                                            @NonNull HttpServletResponse response,
                                            @NonNull FilterChain filterChain) throws ServletException, IOException {

                String role = request.getHeader("X-Role");
                if (role != null && !role.isBlank()) {
                    String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    "gateway-user",
                                    null,
                                    List.of(new SimpleGrantedAuthority(normalized.toUpperCase()))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}

