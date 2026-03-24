package com.shopsphere.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/users")
public class TempUserController {
    @GetMapping("/profile")
    public String getProfile(
            @RequestHeader("X-Email") String email,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-UserId") String userId
    ) {
        return "User Profile → Email: " + email + ", Role: " + role + ", ID: " + userId;
    }
}
