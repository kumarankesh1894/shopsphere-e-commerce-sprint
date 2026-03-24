package com.shopsphere.authservice.service.impl;

import com.shopsphere.authservice.exception.UserNotFoundException;
import com.shopsphere.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



/*
* This will connect DB with Spring Security
* It helps the Authentication Manager to fetch details from  Database
* */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
    }
}
