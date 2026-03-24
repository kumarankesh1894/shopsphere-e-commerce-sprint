package com.shopsphere.authservice.service.impl;

import com.shopsphere.authservice.dto.AuthResponse;
import com.shopsphere.authservice.dto.LoginRequest;
import com.shopsphere.authservice.dto.SignupRequest;
import com.shopsphere.authservice.dto.UserResponse;
import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.enums.Role;
import com.shopsphere.authservice.exception.EmailAlreadyExistsException;
import com.shopsphere.authservice.exception.InvalidCredentialsException;
import com.shopsphere.authservice.exception.UserInactiveException;
import com.shopsphere.authservice.exception.UserNotFoundException;
import com.shopsphere.authservice.repository.UserRepository;
import com.shopsphere.authservice.security.JwtUtil;
import com.shopsphere.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.resource.VersionResourceResolver;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserResponse signup(SignupRequest signupRequest) {
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists");
        }
        //Mapping DTO -> Entity
        User user = modelMapper.map(signupRequest, User.class);

        //overriding improtant fields
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        //Mapping Entity -> DTO
        return modelMapper.map(savedUser, UserResponse.class);
    }
    /*
    * Step 1: Create an authentication request object as This object contains the user's email and password entered during login
    *  It is NOT authenticated yet, just holds credentials
    * Step 2: Pass the authentication request to AuthenticationManager
    * This is the main component of Spring Security responsible for authentication, internally
    * 1. Call UserDetailsService.loadUserByUsername(email)
    * 2. Fetch user from database
    * 3. Use PasswordEncoder to compare passwords
    * 4. If valid → return authenticated object
    * 5. If invalid → throw exception (BadCredentialsException)
    *  Step 3: Extract the authenticated user (principal)
    * After successful authentication, Spring returns a fully authenticated object
    * "principal" contains the actual logged-in user (our User entity)
    *
    * */

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
    //Apply custom business logic, Spring Security does not know about your "active" field, so you must manually check it
        if(!user.getActive()){
            throw new UserInactiveException("User is not active");
        }

        //Generate JWT token and return it in the response
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());


        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(token);

        //mapping user details to response DTO
        authResponse.setUserResponse(modelMapper.map(user, UserResponse.class));
        return authResponse;
    }
}
