package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import com.cts.healthcare_appointment_system.models.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {
    private int auditLogId;
    private Integer actorUserId;
    private String action;
    private String entityType;
    private Integer entityId;
    private String description;
    private LocalDateTime createdAt;

    public static AuditLogResponseDTO from(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getAuditLogId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getCreatedAt());
    }
}
