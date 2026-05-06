package com.cts.healthcare_appointment_system.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cts.healthcare_appointment_system.enums.NotificationStatus;
import com.cts.healthcare_appointment_system.models.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {
    public List<NotificationLog> findByReceiverEmail(String receiverEmail);

    public List<NotificationLog> findByStatus(NotificationStatus status);
}
