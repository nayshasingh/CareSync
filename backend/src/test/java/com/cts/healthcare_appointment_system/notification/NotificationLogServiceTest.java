package com.cts.healthcare_appointment_system.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.cts.healthcare_appointment_system.dto.NotificationLogResponseDTO;
import com.cts.healthcare_appointment_system.enums.NotificationStatus;
import com.cts.healthcare_appointment_system.models.NotificationLog;
import com.cts.healthcare_appointment_system.repositories.NotificationLogRepository;
import com.cts.healthcare_appointment_system.services.NotificationLogService;

@ExtendWith(MockitoExtension.class)
public class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepo;

    @InjectMocks
    private NotificationLogService notificationLogService;

    @Test
    void testRecordSentNotification() {
        notificationLogService.recordSent("patient@example.com", "Appointment confirmed");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepo).save(captor.capture());

        NotificationLog savedLog = captor.getValue();
        assertEquals("patient@example.com", savedLog.getReceiverEmail());
        assertEquals("Appointment confirmed", savedLog.getSubject());
        assertEquals(NotificationStatus.SENT, savedLog.getStatus());
        assertNull(savedLog.getErrorMessage());
    }

    @Test
    void testRecordFailedNotification() {
        notificationLogService.recordFailed("patient@example.com", "Appointment reminder", "SMTP unavailable");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepo).save(captor.capture());

        NotificationLog savedLog = captor.getValue();
        assertEquals("patient@example.com", savedLog.getReceiverEmail());
        assertEquals("Appointment reminder", savedLog.getSubject());
        assertEquals(NotificationStatus.FAILED, savedLog.getStatus());
        assertEquals("SMTP unavailable", savedLog.getErrorMessage());
    }

    @Test
    void testGetNotificationLogsByStatus() {
        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setNotificationLogId(1);
        notificationLog.setReceiverEmail("patient@example.com");
        notificationLog.setSubject("Appointment reminder");
        notificationLog.setStatus(NotificationStatus.SENT);
        notificationLog.setCreatedAt(LocalDateTime.now());

        when(notificationLogRepo.findByStatus(NotificationStatus.SENT)).thenReturn(List.of(notificationLog));

        ResponseEntity<List<NotificationLogResponseDTO>> response = notificationLogService.getNotificationLogs(null, "sent");

        assertEquals(1, response.getBody().size());
        assertEquals(NotificationStatus.SENT, response.getBody().get(0).getStatus());
        verify(notificationLogRepo).findByStatus(NotificationStatus.SENT);
    }
}
