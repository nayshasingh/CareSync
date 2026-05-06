package com.cts.healthcare_appointment_system.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.healthcare_appointment_system.dto.NotificationLogResponseDTO;
import com.cts.healthcare_appointment_system.services.NotificationLogService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/notification-logs")
@AllArgsConstructor
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public ResponseEntity<List<NotificationLogResponseDTO>> getNotificationLogs(
            @RequestParam(required = false) String receiverEmail,
            @RequestParam(required = false) String status) {
        return notificationLogService.getNotificationLogs(receiverEmail, status);
    }
}
