package com.careflow.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_clinic_created", columnList = "clinicId, createdAt"),
        @Index(name = "uk_notifications_event_id", columnList = "eventId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    private UUID patientId;

    private UUID followUpId;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String recipientName;

    private String recipientPhone;

    @Column(nullable = false, length = 2000)
    private String message;

    private String deliveryUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
