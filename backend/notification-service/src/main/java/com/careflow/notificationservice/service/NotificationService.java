package com.careflow.notificationservice.service;

import com.careflow.notificationservice.dto.NotificationResponse;
import com.careflow.notificationservice.entity.Notification;
import com.careflow.notificationservice.entity.NotificationChannel;
import com.careflow.notificationservice.entity.NotificationStatus;
import com.careflow.notificationservice.event.CareFlowEvent;
import com.careflow.notificationservice.exception.NotificationNotFoundException;
import com.careflow.notificationservice.repository.NotificationRepository;
import com.careflow.notificationservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final WhatsAppLinkBuilder whatsAppLinkBuilder;

    @Transactional
    public void handleEvent(CareFlowEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Skipping duplicate eventId={}", event.eventId());
            return;
        }

        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        String patientName = stringValue(payload.get("patientName"));
        String patientPhone = stringValue(payload.get("patientPhone"));
        String message = messageBuilder.buildMessage(event.eventType(), payload);
        String deliveryUrl = whatsAppLinkBuilder.buildLink(patientPhone, message);

        NotificationStatus status = deliveryUrl != null ? NotificationStatus.READY : NotificationStatus.PENDING;

        Notification notification = Notification.builder()
                .clinicId(event.clinicId())
                .patientId(event.patientId())
                .followUpId(event.followUpId())
                .eventId(event.eventId())
                .eventType(event.eventType())
                .channel(NotificationChannel.WHATSAPP_LINK)
                .status(status)
                .recipientName(patientName)
                .recipientPhone(patientPhone)
                .message(message)
                .deliveryUrl(deliveryUrl)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created id={} status={} eventType={}", notification.getId(), status, event.eventType());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAllForCurrentClinic() {
        return notificationRepository.findByClinicIdOrderByCreatedAtDesc(TenantContext.clinicId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markSent(UUID id) {
        Notification notification = notificationRepository.findByIdAndClinicId(id, TenantContext.clinicId())
                .orElseThrow(() -> new NotificationNotFoundException(id));

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
