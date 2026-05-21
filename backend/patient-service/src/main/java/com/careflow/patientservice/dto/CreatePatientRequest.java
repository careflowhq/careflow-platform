package com.careflow.patientservice.dto;

import com.careflow.patientservice.entity.PatientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePatientRequest(
        @NotBlank String fullName,
        @NotBlank String phoneNumber,
        String diagnosis,
        UUID assignedDoctorId,
        PatientStatus status
) {
}
