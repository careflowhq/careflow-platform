package com.careflow.authservice.dto;

public record LoginRequest(
        String email,
        String password
) {
}