package com.careflow.followupservice.scheduler;

import com.careflow.followupservice.entity.FollowUpStatus;
import com.careflow.followupservice.repository.FollowUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Background job that marks overdue follow-ups as MISSED.
 *
 * Scheduling strategy (MVP):
 * - Runs on a fixed cron (default: every 15 minutes, configurable via careflow.followup.overdue-check-cron)
 * - Uses a single bulk UPDATE for all tenants (no TenantContext — system-scoped maintenance)
 * - Only PENDING rows with scheduledDate in the past are affected
 * - Idempotent: re-running does not change COMPLETED, MISSED, or CANCELLED rows
 *
 * Production note: for high volume, consider partitioning by clinicId or publishing
 * domain events instead of polling the full table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueFollowUpScheduler {

    private final FollowUpRepository followUpRepository;

    @Scheduled(cron = "${careflow.followup.overdue-check-cron}")
    @Transactional
    public void markOverdueFollowUpsAsMissed() {
        Instant cutoff = Instant.now();
        int updated = followUpRepository.markOverdueAsMissed(
                cutoff,
                FollowUpStatus.PENDING,
                FollowUpStatus.MISSED
        );

        if (updated > 0) {
            log.info("Marked {} overdue follow-up(s) as MISSED (cutoff={})", updated, cutoff);
        }
    }
}
