package com.careflow.patientservice.service;

import com.careflow.patientservice.dto.CreatePatientRequest;
import com.careflow.patientservice.dto.PatientResponse;
import com.careflow.patientservice.entity.Patient;
import com.careflow.patientservice.entity.PatientStatus;
import com.careflow.patientservice.exception.PatientNotFoundException;
import com.careflow.patientservice.repository.PatientRepository;
import com.careflow.patientservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        UUID clinicId = TenantContext.clinicId();

        Patient patient = Patient.builder()
                .clinicId(clinicId)
                .assignedDoctorId(request.assignedDoctorId())
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .diagnosis(request.diagnosis())
                .status(request.status() != null ? request.status() : PatientStatus.ACTIVE)
                .build();

        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAllForCurrentClinic() {
        UUID clinicId = TenantContext.clinicId();

        return patientRepository.findByClinicIdOrderByCreatedAtDesc(clinicId).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse findByIdForCurrentClinic(UUID id) {
        UUID clinicId = TenantContext.clinicId();

        return patientRepository.findByIdAndClinicId(id, clinicId)
                .map(PatientResponse::from)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }
}
