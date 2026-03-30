package com.shopsphere.authservice.service.impl;

import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.exception.UserNotFoundException;
import com.shopsphere.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        User user = new User();
        user.setEmail("user@shopsphere.com");

        when(userRepository.findByEmail("user@shopsphere.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("user@shopsphere.com");

        assertEquals("user@shopsphere.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_whenMissing_throwsUserNotFoundException() {
        when(userRepository.findByEmail("missing@shopsphere.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@shopsphere.com"));
    }
}

