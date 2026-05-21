package com.careflow.clinicservice.dto;

import com.careflow.clinicservice.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClinicRequest(
        @NotBlank String name,
        @NotBlank String country,
        @NotBlank String timezone,
        @NotNull SubscriptionPlan subscriptionPlan,
        Boolean active
) {
}
