package com.cts.healthcare_appointment_system.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.SystemSummaryDTO;
import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.repositories.AppointmentRepository;
import com.cts.healthcare_appointment_system.repositories.AvailabilityRepository;
import com.cts.healthcare_appointment_system.repositories.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReportService {

    private final UserRepository userRepo;
    private final AppointmentRepository appointmentRepo;
    private final AvailabilityRepository availabilityRepo;

    public ResponseEntity<SystemSummaryDTO> getSystemSummary() {
        long totalAvailabilitySlots = availabilityRepo.count();
        long availableSlots = availabilityRepo.countByAvailabilityStatus(true);

        SystemSummaryDTO summary = new SystemSummaryDTO(
                userRepo.count(),
                userRepo.countByRole(UserRole.PATIENT),
                userRepo.countByRole(UserRole.DOCTOR),
                appointmentRepo.count(),
                appointmentRepo.countByStatus(AppointmentStatus.BOOKED),
                appointmentRepo.countByStatus(AppointmentStatus.COMPLETED),
                appointmentRepo.countByStatus(AppointmentStatus.CANCELLED),
                totalAvailabilitySlots,
                availableSlots,
                totalAvailabilitySlots - availableSlots);

        return ResponseEntity.status(HttpStatus.OK).body(summary);
    }
}
