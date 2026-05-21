package com.careflow.clinicservice.repository;

import com.careflow.clinicservice.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    List<Clinic> findByActiveTrueOrderByNameAsc();

    Optional<Clinic> findByIdAndActiveTrue(UUID id);
}
