package com.careflow.followupservice.repository;

import com.careflow.followupservice.entity.FollowUp;
import com.careflow.followupservice.entity.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {

    List<FollowUp> findByClinicIdOrderByScheduledDateDesc(UUID clinicId);

    List<FollowUp> findByClinicIdAndStatusOrderByScheduledDateAsc(UUID clinicId, FollowUpStatus status);

    Optional<FollowUp> findByIdAndClinicId(UUID id, UUID clinicId);

    List<FollowUp> findByStatusAndScheduledDateBefore(FollowUpStatus status, Instant cutoff);

    /**
     * Used by the overdue scheduler: transitions all overdue PENDING rows across tenants.
     * Tenant isolation at read-time is enforced by clinicId on each row; the job is system-scoped.
     */
    @Modifying
    @Query("""
            UPDATE FollowUp f
            SET f.status = :missedStatus
            WHERE f.status = :pendingStatus
              AND f.scheduledDate < :cutoff
            """)
    int markOverdueAsMissed(
            @Param("cutoff") Instant cutoff,
            @Param("pendingStatus") FollowUpStatus pendingStatus,
            @Param("missedStatus") FollowUpStatus missedStatus
    );
}
