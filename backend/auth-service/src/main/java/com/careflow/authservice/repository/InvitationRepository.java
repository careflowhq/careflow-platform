package com.careflow.authservice.repository;

import com.careflow.authservice.entity.Invitation;
import com.careflow.authservice.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenAndStatus(String token, InvitationStatus status);

    boolean existsByEmailAndClinicIdAndStatus(String email, UUID clinicId, InvitationStatus status);
}
