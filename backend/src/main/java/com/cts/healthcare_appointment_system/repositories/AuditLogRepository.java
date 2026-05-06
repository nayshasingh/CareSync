package com.cts.healthcare_appointment_system.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cts.healthcare_appointment_system.models.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    public List<AuditLog> findByActorUserId(Integer actorUserId);

    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, Integer entityId);
}
