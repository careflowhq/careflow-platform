package com.careflow.followupservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Follow-up aggregate scoped to a single clinic.
 *
 * Tenant isolation: clinicId is the partition key; every query must filter by it.
 * clinicId and createdBy are derived from gateway headers, never from client payloads.
 */
@Entity
@Table(name = "followups", indexes = {
        @Index(name = "idx_followups_clinic_status", columnList = "clinicId, status"),
        @Index(name = "idx_followups_clinic_scheduled", columnList = "clinicId, scheduledDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private UUID patientId;

    private UUID doctorId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Instant scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus status;

    private String notes;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = FollowUpStatus.PENDING;
        }
    }
}
