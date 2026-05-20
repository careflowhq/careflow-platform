package com.careflow.authservice.dto;

import com.careflow.authservice.entity.UserRole;

public record RegisterRequest(
        String fullName,
        String email,
        String password,
        UserRole role
) {
}