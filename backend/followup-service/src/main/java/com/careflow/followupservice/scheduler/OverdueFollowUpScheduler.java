package com.careflow.followupservice.scheduler;

import com.careflow.followupservice.client.PatientServiceClient;
import com.careflow.followupservice.client.PatientSummary;
import com.careflow.followupservice.entity.FollowUp;
import com.careflow.followupservice.entity.FollowUpStatus;
import com.careflow.followupservice.messaging.FollowUpEventPublisher;
import com.careflow.followupservice.repository.FollowUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueFollowUpScheduler {

    private final FollowUpRepository followUpRepository;
    private final PatientServiceClient patientServiceClient;
    private final FollowUpEventPublisher eventPublisher;

    @Scheduled(cron = "${careflow.followup.overdue-check-cron}")
    @Transactional
    public void markOverdueFollowUpsAsMissed() {
        Instant cutoff = Instant.now();
        List<FollowUp> overdue = followUpRepository.findByStatusAndScheduledDateBefore(
                FollowUpStatus.PENDING, cutoff);

        if (overdue.isEmpty()) {
            return;
        }

        for (FollowUp followUp : overdue) {
            followUp.setStatus(FollowUpStatus.MISSED);
            followUpRepository.save(followUp);

            PatientSummary patient = patientServiceClient.getPatient(
                    followUp.getPatientId(),
                    followUp.getClinicId(),
                    followUp.getCreatedBy(),
                    "CLINIC_ADMIN"
            );
            eventPublisher.publishMissed(followUp, patient);
        }

        log.info("Marked {} overdue follow-up(s) as MISSED (cutoff={})", overdue.size(), cutoff);
    }
}
