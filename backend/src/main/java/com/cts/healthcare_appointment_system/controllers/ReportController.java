package com.cts.healthcare_appointment_system.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.healthcare_appointment_system.dto.SystemSummaryDTO;
import com.cts.healthcare_appointment_system.services.ReportService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/reports")
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<SystemSummaryDTO> getSystemSummary() {
        return reportService.getSystemSummary();
    }
}
