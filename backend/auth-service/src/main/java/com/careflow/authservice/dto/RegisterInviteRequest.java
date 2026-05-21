package com.careflow.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterInviteRequest(
        @NotBlank String token,
        @NotBlank String password
) {
}
