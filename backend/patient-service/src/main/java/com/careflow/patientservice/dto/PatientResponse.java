package com.careflow.patientservice.dto;

import com.careflow.patientservice.entity.Patient;
import com.careflow.patientservice.entity.PatientStatus;

import java.time.Instant;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        UUID clinicId,
        UUID assignedDoctorId,
        String fullName,
        String phoneNumber,
        String diagnosis,
        PatientStatus status,
        Instant createdAt
) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getClinicId(),
                patient.getAssignedDoctorId(),
                patient.getFullName(),
                patient.getPhoneNumber(),
                patient.getDiagnosis(),
                patient.getStatus(),
                patient.getCreatedAt()
        );
    }
}
