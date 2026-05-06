package com.cts.healthcare_appointment_system.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.NotificationLogResponseDTO;
import com.cts.healthcare_appointment_system.enums.NotificationStatus;
import com.cts.healthcare_appointment_system.error.ApiException;
import com.cts.healthcare_appointment_system.models.NotificationLog;
import com.cts.healthcare_appointment_system.repositories.NotificationLogRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepo;

    public void recordSent(String receiverEmail, String subject) {
        record(receiverEmail, subject, NotificationStatus.SENT, null);
    }

    public void recordFailed(String receiverEmail, String subject, String errorMessage) {
        record(receiverEmail, subject, NotificationStatus.FAILED, errorMessage);
    }

    public ResponseEntity<List<NotificationLogResponseDTO>> getNotificationLogs(String receiverEmail, String status) {
        List<NotificationLog> notificationLogs;
        if (receiverEmail != null && !receiverEmail.isBlank()) {
            notificationLogs = notificationLogRepo.findByReceiverEmail(receiverEmail);
        } else if (status != null && !status.isBlank()) {
            notificationLogs = notificationLogRepo.findByStatus(parseStatus(status));
        } else {
            notificationLogs = notificationLogRepo.findAll(Sort.by(Direction.DESC, "createdAt"));
        }

        if (notificationLogs.isEmpty()) {
            throw new ApiException("No notification logs found", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(notificationLogs.stream().map(NotificationLogResponseDTO::from).toList());
    }

    private void record(String receiverEmail, String subject, NotificationStatus status, String errorMessage) {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setReceiverEmail(receiverEmail);
        notificationLog.setSubject(subject);
        notificationLog.setStatus(status);
        notificationLog.setErrorMessage(errorMessage);
        notificationLog.setCreatedAt(LocalDateTime.now());
        notificationLogRepo.save(notificationLog);
    }

    private NotificationStatus parseStatus(String status) {
        try {
            return NotificationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid notification status: " + status, HttpStatus.BAD_REQUEST);
        }
    }
}
