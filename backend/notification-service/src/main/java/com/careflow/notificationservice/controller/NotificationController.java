package com.careflow.notificationservice.controller;

import com.careflow.notificationservice.dto.NotificationResponse;
import com.careflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> findAll() {
        return notificationService.findAllForCurrentClinic();
    }

    @PostMapping("/{id}/send")
    public NotificationResponse markSent(@PathVariable UUID id) {
        return notificationService.markSent(id);
    }
}
