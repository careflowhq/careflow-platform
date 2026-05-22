package com.careflow.followupservice.client;

import java.util.UUID;

public record PatientSummary(
        UUID id,
        String fullName,
        String phoneNumber
) {
}
