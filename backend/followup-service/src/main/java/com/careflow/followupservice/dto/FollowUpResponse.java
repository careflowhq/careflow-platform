package com.careflow.followupservice.dto;

import com.careflow.followupservice.entity.FollowUp;
import com.careflow.followupservice.entity.FollowUpStatus;

import java.time.Instant;
import java.util.UUID;

public record FollowUpResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        UUID doctorId,
        String type,
        Instant scheduledDate,
        FollowUpStatus status,
        String notes,
        UUID createdBy,
        Instant createdAt
) {

    public static FollowUpResponse from(FollowUp followUp) {
        return new FollowUpResponse(
                followUp.getId(),
                followUp.getClinicId(),
                followUp.getPatientId(),
                followUp.getDoctorId(),
                followUp.getType(),
                followUp.getScheduledDate(),
                followUp.getStatus(),
                followUp.getNotes(),
                followUp.getCreatedBy(),
                followUp.getCreatedAt()
        );
    }
}
