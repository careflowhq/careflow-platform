package com.careflow.patientservice.repository;

import com.careflow.patientservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findByClinicIdOrderByCreatedAtDesc(UUID clinicId);

    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);
}
