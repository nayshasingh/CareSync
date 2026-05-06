package com.cts.healthcare_appointment_system.dto;

import com.cts.healthcare_appointment_system.models.Consultation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponseDTO {
    private int consultationId;
    private AppointmentResponseDTO appointment;
    private String notes;
    private String prescription;

    public static ConsultationResponseDTO from(Consultation consultation) {
        return new ConsultationResponseDTO(
                consultation.getConsultationId(),
                consultation.getAppointment() == null ? null : AppointmentResponseDTO.from(consultation.getAppointment()),
                consultation.getNotes(),
                consultation.getPrescription());
    }
}
