package com.shopsphere.authservice.service.impl;

import com.shopsphere.authservice.dto.AuthResponse;
import com.shopsphere.authservice.dto.LoginRequest;
import com.shopsphere.authservice.dto.SignupRequest;
import com.shopsphere.authservice.dto.UserResponse;
import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.enums.Role;
import com.shopsphere.authservice.exception.EmailAlreadyExistsException;
import com.shopsphere.authservice.exception.UserInactiveException;
import com.shopsphere.authservice.repository.UserRepository;
import com.shopsphere.authservice.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void signup_whenEmailAlreadyExists_throwsEmailAlreadyExistsException() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("taken@shopsphere.com");

        when(userRepository.existsByEmail("taken@shopsphere.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signup_whenValidRequest_setsSecurityFieldsAndReturnsResponse() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setName("Demo User");
        signupRequest.setEmail("demo@shopsphere.com");
        signupRequest.setPassword("plain-pass");

        User mappedUser = new User();
        mappedUser.setName("Demo User");
        mappedUser.setEmail("demo@shopsphere.com");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setName("Demo User");
        savedUser.setEmail("demo@shopsphere.com");
        savedUser.setRole(Role.USER);
        savedUser.setActive(true);

        UserResponse mappedResponse = new UserResponse();
        mappedResponse.setId(10L);
        mappedResponse.setEmail("demo@shopsphere.com");

        when(userRepository.existsByEmail("demo@shopsphere.com")).thenReturn(false);
        when(modelMapper.map(signupRequest, User.class)).thenReturn(mappedUser);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(savedUser, UserResponse.class)).thenReturn(mappedResponse);

        UserResponse response = authService.signup(signupRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals("encoded-pass", userCaptor.getValue().getPassword());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
        assertTrue(userCaptor.getValue().getActive());
        assertEquals(10L, response.getId());
    }

    @Test
    void login_whenUserInactive_throwsUserInactiveException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("inactive@shopsphere.com");
        loginRequest.setPassword("pass");

        User inactiveUser = new User();
        inactiveUser.setId(9L);
        inactiveUser.setEmail("inactive@shopsphere.com");
        inactiveUser.setRole(Role.USER);
        inactiveUser.setActive(false);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(inactiveUser);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        assertThrows(UserInactiveException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_whenCredentialsValid_returnsTokenAndUserResponse() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@shopsphere.com");
        loginRequest.setPassword("pass");

        User activeUser = new User();
        activeUser.setId(7L);
        activeUser.setEmail("user@shopsphere.com");
        activeUser.setRole(Role.USER);
        activeUser.setActive(true);

        UserResponse userResponse = new UserResponse();
        userResponse.setId(7L);
        userResponse.setEmail("user@shopsphere.com");

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(activeUser);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken(7L, "user@shopsphere.com", "USER")).thenReturn("jwt-token");
        when(modelMapper.map(activeUser, UserResponse.class)).thenReturn(userResponse);

        AuthResponse response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(7L, response.getUserResponse().getId());
    }
}

