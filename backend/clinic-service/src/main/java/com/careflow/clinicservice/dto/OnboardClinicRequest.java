package com.careflow.clinicservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardClinicRequest(
        @NotBlank String name,
        @NotBlank String country,
        @NotBlank String timezone
) {
}
