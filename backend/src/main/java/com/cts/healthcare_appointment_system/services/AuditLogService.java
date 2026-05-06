package com.cts.healthcare_appointment_system.services;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.AuditLogResponseDTO;
import com.cts.healthcare_appointment_system.error.ApiException;
import com.cts.healthcare_appointment_system.models.AuditLog;
import com.cts.healthcare_appointment_system.repositories.AuditLogRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;

    public void record(Integer actorUserId, String action, String entityType, Integer entityId, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        auditLogRepo.save(auditLog);
    }

    public ResponseEntity<List<AuditLogResponseDTO>> getAuditLogs(Integer actorUserId, String entityType, Integer entityId) {
        List<AuditLog> auditLogs;

        if (entityType != null && entityId != null) {
            auditLogs = auditLogRepo.findByEntityTypeAndEntityId(entityType, entityId);
        } else if (actorUserId != null) {
            auditLogs = auditLogRepo.findByActorUserId(actorUserId);
        } else {
            auditLogs = auditLogRepo.findAll(Sort.by(Direction.DESC, "createdAt"));
        }

        if (auditLogs.isEmpty()) {
            throw new ApiException("No audit logs found", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(auditLogs.stream().map(AuditLogResponseDTO::from).toList());
    }
}
