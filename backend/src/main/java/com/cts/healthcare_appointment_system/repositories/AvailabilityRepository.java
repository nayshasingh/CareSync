package com.cts.healthcare_appointment_system.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cts.healthcare_appointment_system.models.Availability;
public interface AvailabilityRepository extends JpaRepository<Availability, Integer>{
    public Optional<Availability> findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(int doctorId, LocalDateTime timeSlotSlart, LocalDateTime timeSlotEnd);

    @Query("SELECT COUNT(a) FROM Availability a WHERE a.isAvailable = :available")
    public long countByAvailabilityStatus(@Param("available") boolean available);

    @Query("""
            SELECT COUNT(a) > 0 FROM Availability a
            WHERE a.doctor.userId = :doctorId
              AND (:excludedAvailabilityId IS NULL OR a.availabilityId <> :excludedAvailabilityId)
              AND a.timeSlotStart < :timeSlotEnd
              AND a.timeSlotEnd > :timeSlotStart
            """)
    public boolean existsDoctorAvailabilityOverlap(
            @Param("doctorId") int doctorId,
            @Param("timeSlotStart") LocalDateTime timeSlotStart,
            @Param("timeSlotEnd") LocalDateTime timeSlotEnd,
            @Param("excludedAvailabilityId") Integer excludedAvailabilityId);

    // Find all the past availability slots of the current date
    @Query(value = "SELECT * FROM availabilities WHERE DATE(time_slot_start) = CURRENT_DATE AND time_slot_end <= CURRENT_TIMESTAMP", nativeQuery = true)
    public List<Availability> findPastSlotsOfToday();
}
