package com.careflow.followupservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CareFlowEvent(
        String eventType,
        UUID eventId,
        Instant occurredAt,
        UUID clinicId,
        UUID patientId,
        UUID followUpId,
        Map<String, Object> payload
) {
}
