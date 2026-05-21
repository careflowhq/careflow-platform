package com.careflow.followupservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UpdateFollowUpRequest(
        UUID doctorId,
        @NotBlank String type,
        @NotNull Instant scheduledDate,
        String notes
) {
}
