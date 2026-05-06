package com.cts.healthcare_appointment_system.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cts.healthcare_appointment_system.dto.SystemSummaryDTO;
import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.repositories.AppointmentRepository;
import com.cts.healthcare_appointment_system.repositories.AvailabilityRepository;
import com.cts.healthcare_appointment_system.repositories.UserRepository;
import com.cts.healthcare_appointment_system.services.ReportService;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private AppointmentRepository appointmentRepo;

    @Mock
    private AvailabilityRepository availabilityRepo;

    @InjectMocks
    private ReportService reportService;

    @Test
    void testGetSystemSummary() {
        when(userRepo.count()).thenReturn(10L);
        when(userRepo.countByRole(UserRole.PATIENT)).thenReturn(7L);
        when(userRepo.countByRole(UserRole.DOCTOR)).thenReturn(3L);
        when(appointmentRepo.count()).thenReturn(20L);
        when(appointmentRepo.countByStatus(AppointmentStatus.BOOKED)).thenReturn(8L);
        when(appointmentRepo.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(9L);
        when(appointmentRepo.countByStatus(AppointmentStatus.CANCELLED)).thenReturn(3L);
        when(availabilityRepo.count()).thenReturn(12L);
        when(availabilityRepo.countByAvailabilityStatus(true)).thenReturn(5L);

        SystemSummaryDTO summary = reportService.getSystemSummary().getBody();

        assertEquals(10L, summary.getTotalUsers());
        assertEquals(7L, summary.getTotalPatients());
        assertEquals(3L, summary.getTotalDoctors());
        assertEquals(20L, summary.getTotalAppointments());
        assertEquals(8L, summary.getBookedAppointments());
        assertEquals(9L, summary.getCompletedAppointments());
        assertEquals(3L, summary.getCancelledAppointments());
        assertEquals(12L, summary.getTotalAvailabilitySlots());
        assertEquals(5L, summary.getAvailableSlots());
        assertEquals(7L, summary.getUnavailableSlots());
    }
}
