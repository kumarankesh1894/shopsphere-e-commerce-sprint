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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
       User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()-> new UserNotFoundException("User not found with this email: "+ loginRequest.getEmail()));
        if(!user.getActive()){
            throw new UserInactiveException("User is not active");
        }
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid credentials");
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
