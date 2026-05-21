package com.careflow.clinicservice.dto;

import com.careflow.clinicservice.entity.Clinic;
import com.careflow.clinicservice.entity.SubscriptionPlan;

import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        String country,
        String timezone,
        SubscriptionPlan subscriptionPlan,
        boolean active
) {

    public static ClinicResponse from(Clinic clinic) {
        return new ClinicResponse(
                clinic.getId(),
                clinic.getName(),
                clinic.getCountry(),
                clinic.getTimezone(),
                clinic.getSubscriptionPlan(),
                clinic.isActive()
        );
    }
}
