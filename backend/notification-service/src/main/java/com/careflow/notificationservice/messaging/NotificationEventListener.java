package com.careflow.notificationservice.messaging;

import com.careflow.notificationservice.event.CareFlowEvent;
import com.careflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${careflow.rabbitmq.queue}")
    public void onCareFlowEvent(CareFlowEvent event) {
        log.info("Received event type={} eventId={}", event.eventType(), event.eventId());
        notificationService.handleEvent(event);
    }
}
