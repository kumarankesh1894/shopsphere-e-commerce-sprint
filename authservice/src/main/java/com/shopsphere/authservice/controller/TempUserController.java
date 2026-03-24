package com.shopsphere.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class TempUserController {
    @GetMapping("/users/profile")
    public String getProfile(
            @RequestHeader("X-Email") String email,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-UserId") String userId
    ) {
        return "User Profile → Email: " + email + ", Role: " + role + ", ID: " + userId;
    }

    // Basic admin test endpoint
    @GetMapping("/admin/dashboard")
    public String getAdminDashboard(
            @RequestHeader("X-Email") String email,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-UserId") String userId
    ) {
        return "Admin Dashboard → Email: " + email + ", Role: " + role + ", ID: " + userId;
    }

    // Another admin-only endpoint (optional)
    @GetMapping("/admin/stats")
    public String getStats(
            @RequestHeader("X-Email") String email,
            @RequestHeader("X-Role") String role
    ) {
        return "Admin Stats → Access granted to: " + email + " with role: " + role;
    }
}
