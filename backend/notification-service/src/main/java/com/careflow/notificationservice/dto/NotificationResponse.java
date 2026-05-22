package com.careflow.notificationservice.dto;

import com.careflow.notificationservice.entity.Notification;
import com.careflow.notificationservice.entity.NotificationChannel;
import com.careflow.notificationservice.entity.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID clinicId,
        UUID patientId,
        UUID followUpId,
        String eventType,
        NotificationChannel channel,
        NotificationStatus status,
        String recipientName,
        String recipientPhone,
        String message,
        String deliveryUrl,
        Instant createdAt,
        Instant sentAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getClinicId(),
                notification.getPatientId(),
                notification.getFollowUpId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getRecipientName(),
                notification.getRecipientPhone(),
                notification.getMessage(),
                notification.getDeliveryUrl(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
