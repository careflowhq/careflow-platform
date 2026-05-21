package com.careflow.clinicservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Clinic aggregate — top-level tenant in CareFlow.
 * All downstream domain data (patients, follow-ups) belongs to a clinic.
 */
@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    void onCreate() {
        if (subscriptionPlan == null) {
            subscriptionPlan = SubscriptionPlan.FREE;
        }
    }
}
