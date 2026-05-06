package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import com.cts.healthcare_appointment_system.models.Availability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponseDTO {
    private int availabilityId;
    private UserResponseDTO doctor;
    private LocalDateTime timeSlotStart;
    private LocalDateTime timeSlotEnd;
    private boolean available;

    public static AvailabilityResponseDTO from(Availability availability) {
        return new AvailabilityResponseDTO(
                availability.getAvailabilityId(),
                availability.getDoctor() == null ? null : UserResponseDTO.from(availability.getDoctor()),
                availability.getTimeSlotStart(),
                availability.getTimeSlotEnd(),
                availability.isAvailable());
    }
}
