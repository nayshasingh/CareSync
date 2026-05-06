package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRescheduleDTO {

    @NotNull(message = "Appointment id is required")
    @Positive(message = "Appointment id must be positive")
    private Integer appointmentId;

    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be positive")
    private Integer doctorId;

    @NotNull(message = "Time slot start cannot be null")
    @Future(message = "Time slot start must be in the future")
    private LocalDateTime timeSlotStart;

    @NotNull(message = "Time slot end cannot be null")
    @Future(message = "Time slot end must be in the future")
    private LocalDateTime timeSlotEnd;
}
