package com.careflow.authservice.dto;

import com.careflow.authservice.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull UserRole role
) {
}
