package com.careflow.apigateway.security;

import java.util.UUID;

public record JwtClaims(UUID userId, UUID clinicId, String role) {
}
