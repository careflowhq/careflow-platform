package com.careflow.authservice.dto;

import com.careflow.authservice.entity.Invitation;
import com.careflow.authservice.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
        String token,
        String email,
        String fullName,
        UserRole role,
        UUID clinicId,
        Instant expiresAt
) {

    public static InviteResponse from(Invitation invitation) {
        return new InviteResponse(
                invitation.getToken(),
                invitation.getEmail(),
                invitation.getFullName(),
                invitation.getRole(),
                invitation.getClinicId(),
                invitation.getExpiresAt()
        );
    }
}
