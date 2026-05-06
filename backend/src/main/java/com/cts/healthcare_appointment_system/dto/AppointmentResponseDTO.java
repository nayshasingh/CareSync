package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.models.Appointment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {
    private int appointmentId;
    private UserResponseDTO patient;
    private UserResponseDTO doctor;
    private LocalDateTime timeSlotStart;
    private LocalDateTime timeSlotEnd;
    private AppointmentStatus status;

    public static AppointmentResponseDTO from(Appointment appointment) {
        return new AppointmentResponseDTO(
                appointment.getAppointmentId(),
                appointment.getPatient() == null ? null : UserResponseDTO.from(appointment.getPatient()),
                appointment.getDoctor() == null ? null : UserResponseDTO.from(appointment.getDoctor()),
                appointment.getTimeSlotStart(),
                appointment.getTimeSlotEnd(),
                appointment.getStatus());
    }
}
