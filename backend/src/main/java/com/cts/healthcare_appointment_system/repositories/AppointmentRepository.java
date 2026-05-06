package com.cts.healthcare_appointment_system.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.models.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer>{
    public List<Appointment> findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(int doctorId, LocalDateTime timeSlotSlart, LocalDateTime timeSlotEnd);

    // Fetch all the appointments between the date-time range
    public List<Appointment> findByTimeSlotStartBetween(LocalDateTime start, LocalDateTime end);

    public long countByStatus(AppointmentStatus status);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.patient.userId = :patientId
              AND a.status = com.cts.healthcare_appointment_system.enums.AppointmentStatus.BOOKED
              AND (:excludedAppointmentId IS NULL OR a.appointmentId <> :excludedAppointmentId)
              AND a.timeSlotStart < :timeSlotEnd
              AND a.timeSlotEnd > :timeSlotStart
            """)
    public boolean existsBookedPatientOverlap(
            @Param("patientId") int patientId,
            @Param("timeSlotStart") LocalDateTime timeSlotStart,
            @Param("timeSlotEnd") LocalDateTime timeSlotEnd,
            @Param("excludedAppointmentId") Integer excludedAppointmentId);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.doctor.userId = :doctorId
              AND a.status = com.cts.healthcare_appointment_system.enums.AppointmentStatus.BOOKED
              AND (:excludedAppointmentId IS NULL OR a.appointmentId <> :excludedAppointmentId)
              AND a.timeSlotStart < :timeSlotEnd
              AND a.timeSlotEnd > :timeSlotStart
            """)
    public boolean existsBookedDoctorOverlap(
            @Param("doctorId") int doctorId,
            @Param("timeSlotStart") LocalDateTime timeSlotStart,
            @Param("timeSlotEnd") LocalDateTime timeSlotEnd,
            @Param("excludedAppointmentId") Integer excludedAppointmentId);
}
