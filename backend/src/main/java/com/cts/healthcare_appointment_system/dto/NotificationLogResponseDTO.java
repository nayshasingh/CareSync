package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import com.cts.healthcare_appointment_system.enums.NotificationStatus;
import com.cts.healthcare_appointment_system.models.NotificationLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogResponseDTO {
    private int notificationLogId;
    private String receiverEmail;
    private String subject;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static NotificationLogResponseDTO from(NotificationLog notificationLog) {
        return new NotificationLogResponseDTO(
                notificationLog.getNotificationLogId(),
                notificationLog.getReceiverEmail(),
                notificationLog.getSubject(),
                notificationLog.getStatus(),
                notificationLog.getErrorMessage(),
                notificationLog.getCreatedAt());
    }
}
