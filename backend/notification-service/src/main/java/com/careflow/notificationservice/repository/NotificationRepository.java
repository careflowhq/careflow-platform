package com.careflow.notificationservice.repository;

import com.careflow.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByClinicIdOrderByCreatedAtDesc(UUID clinicId);

    Optional<Notification> findByIdAndClinicId(UUID id, UUID clinicId);

    boolean existsByEventId(UUID eventId);
}
