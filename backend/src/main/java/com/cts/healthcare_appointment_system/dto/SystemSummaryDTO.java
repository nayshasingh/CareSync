package com.cts.healthcare_appointment_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSummaryDTO {
    private long totalUsers;
    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long bookedAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private long totalAvailabilitySlots;
    private long availableSlots;
    private long unavailableSlots;
}
