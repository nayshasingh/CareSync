package com.cts.healthcare_appointment_system.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.healthcare_appointment_system.dto.AuditLogResponseDTO;
import com.cts.healthcare_appointment_system.services.AuditLogService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/audit-logs")
public class AuditLogController {

    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> getAuditLogs(
            @RequestParam(required = false) Integer actorUserId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Integer entityId) {
        return auditLogService.getAuditLogs(actorUserId, entityType, entityId);
    }
}
