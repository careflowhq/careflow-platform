package com.careflow.patientservice.dto;

import com.careflow.patientservice.entity.PatientStatus;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdatePatientRequest(
        @NotBlank String fullName,
        @NotBlank String phoneNumber,
        String diagnosis,
        UUID assignedDoctorId,
        PatientStatus status
) {
}
