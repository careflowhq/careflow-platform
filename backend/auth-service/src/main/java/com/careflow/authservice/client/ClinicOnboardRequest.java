package com.careflow.authservice.client;

import java.util.UUID;

public record ClinicOnboardRequest(
        String name,
        String country,
        String timezone
) {
}
