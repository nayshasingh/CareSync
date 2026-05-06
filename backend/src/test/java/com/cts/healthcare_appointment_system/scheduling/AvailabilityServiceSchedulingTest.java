package com.cts.healthcare_appointment_system.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.cts.healthcare_appointment_system.dto.AvailabilityResponseDTO;
import com.cts.healthcare_appointment_system.dto.AvailabilitySlotGenerationDTO;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.models.Availability;
import com.cts.healthcare_appointment_system.models.User;
import com.cts.healthcare_appointment_system.repositories.AppointmentRepository;
import com.cts.healthcare_appointment_system.repositories.AvailabilityRepository;
import com.cts.healthcare_appointment_system.repositories.UserRepository;
import com.cts.healthcare_appointment_system.services.AvailabilityService;
import com.cts.healthcare_appointment_system.services.NotificationService;

@ExtendWith(MockitoExtension.class)
public class AvailabilityServiceSchedulingTest {

    @Mock
    private AvailabilityRepository availabilityRepo;

    @Mock
    private AppointmentRepository appointmentRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void testGenerateAvailabilitySlotsSkipsOverlappingSlot() {
        LocalDateTime windowStart = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime windowEnd = windowStart.plusHours(2);
        AvailabilitySlotGenerationDTO dto = new AvailabilitySlotGenerationDTO(2, windowStart, windowEnd, 30, 0);
        User doctor = createDoctor();

        when(userRepo.findById(2)).thenReturn(Optional.of(doctor));
        when(availabilityRepo.existsDoctorAvailabilityOverlap(2, windowStart, windowStart.plusMinutes(30), null)).thenReturn(true);

        ResponseEntity<List<AvailabilityResponseDTO>> response = availabilityService.generateAvailabilitySlots(dto);

        assertEquals(3, response.getBody().size());
        assertEquals(windowStart.plusMinutes(30), response.getBody().get(0).getTimeSlotStart());

        verify(availabilityRepo).saveAll(argThat(slots -> countSlots(slots) == 3));
        verify(userRepo).save(doctor);
    }

    private int countSlots(Iterable<Availability> slots) {
        int count = 0;
        for (Availability slot : slots) {
            count++;
        }
        return count;
    }

    private User createDoctor() {
        User doctor = new User();
        doctor.setUserId(2);
        doctor.setName("Doctor");
        doctor.setEmail("doctor@example.com");
        doctor.setRole(UserRole.DOCTOR);
        return doctor;
    }
}
