package com.careflow.followupservice.messaging;

import com.careflow.followupservice.client.PatientSummary;
import com.careflow.followupservice.entity.FollowUp;
import com.careflow.followupservice.event.CareFlowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${careflow.rabbitmq.exchange}")
    private String exchange;

    public void publishScheduled(FollowUp followUp, PatientSummary patient) {
        publish("followup.scheduled", followUp, patient);
    }

    public void publishMissed(FollowUp followUp, PatientSummary patient) {
        publish("followup.missed", followUp, patient);
    }

    private void publish(String eventType, FollowUp followUp, PatientSummary patient) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", followUp.getType());
        payload.put("scheduledDate", followUp.getScheduledDate().toString());
        if (patient != null) {
            payload.put("patientName", patient.fullName());
            payload.put("patientPhone", patient.phoneNumber());
        }

        CareFlowEvent event = new CareFlowEvent(
                eventType,
                UUID.randomUUID(),
                Instant.now(),
                followUp.getClinicId(),
                followUp.getPatientId(),
                followUp.getId(),
                payload
        );

        rabbitTemplate.convertAndSend(exchange, eventType, event);
        log.info("Published {} for followUpId={}", eventType, followUp.getId());
    }
}
