package com.cts.healthcare_appointment_system.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cts.healthcare_appointment_system.dto.AppointmentDTO;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.error.ApiException;
import com.cts.healthcare_appointment_system.models.Appointment;
import com.cts.healthcare_appointment_system.models.Availability;
import com.cts.healthcare_appointment_system.models.User;
import com.cts.healthcare_appointment_system.repositories.AppointmentRepository;
import com.cts.healthcare_appointment_system.repositories.AvailabilityRepository;
import com.cts.healthcare_appointment_system.repositories.UserRepository;
import com.cts.healthcare_appointment_system.services.AppointmentService;
import com.cts.healthcare_appointment_system.services.AuditLogService;
import com.cts.healthcare_appointment_system.services.NotificationService;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceSchedulingTest {

    @Mock
    private AppointmentRepository appointmentRepo;

    @Mock
    private AvailabilityRepository availabilityRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void testSaveAppointmentRejectsPatientOverlap() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(30);
        AppointmentDTO dto = new AppointmentDTO(1, 2, start, end);

        User patient = createUser(1, UserRole.PATIENT);
        User doctor = createUser(2, UserRole.DOCTOR);
        Availability availability = createAvailability(doctor, start, end);

        when(userRepo.findById(1)).thenReturn(Optional.of(patient));
        when(userRepo.findById(2)).thenReturn(Optional.of(doctor));
        when(availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(2, start, end)).thenReturn(Optional.of(availability));
        when(appointmentRepo.existsBookedPatientOverlap(1, start, end, null)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> appointmentService.saveAppointment(dto));

        assertEquals("Patient already has a booked appointment in this time slot", exception.getMessage());
        verify(appointmentRepo, never()).save(org.mockito.ArgumentMatchers.any(Appointment.class));
    }

    @Test
    void testSaveAppointmentRejectsDoctorOverlap() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(30);
        AppointmentDTO dto = new AppointmentDTO(1, 2, start, end);

        User patient = createUser(1, UserRole.PATIENT);
        User doctor = createUser(2, UserRole.DOCTOR);
        Availability availability = createAvailability(doctor, start, end);

        when(userRepo.findById(1)).thenReturn(Optional.of(patient));
        when(userRepo.findById(2)).thenReturn(Optional.of(doctor));
        when(availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(2, start, end)).thenReturn(Optional.of(availability));
        when(appointmentRepo.existsBookedPatientOverlap(1, start, end, null)).thenReturn(false);
        when(appointmentRepo.existsBookedDoctorOverlap(2, start, end, null)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> appointmentService.saveAppointment(dto));

        assertEquals("Doctor already has a booked appointment in this time slot", exception.getMessage());
        verify(appointmentRepo, never()).save(org.mockito.ArgumentMatchers.any(Appointment.class));
    }

    private User createUser(int userId, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setName(role.name().toLowerCase());
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setRole(role);
        return user;
    }

    private Availability createAvailability(User doctor, LocalDateTime start, LocalDateTime end) {
        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setTimeSlotStart(start);
        availability.setTimeSlotEnd(end);
        availability.setAvailable(true);
        return availability;
    }
}
