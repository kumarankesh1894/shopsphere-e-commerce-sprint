package com.shopsphere.authservice.config;

import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.enums.Role;
import com.shopsphere.authservice.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init(){
        String adminEmail = "admin@gmail.com";
                //check if admin already exists to avoid duplicate entries on application restart
        if(!userRepository.existsByEmail(adminEmail)){
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);

        }
    }
}
