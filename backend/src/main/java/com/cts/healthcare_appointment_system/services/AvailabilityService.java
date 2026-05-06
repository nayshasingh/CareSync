package com.cts.healthcare_appointment_system.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.AvailabilityDTO;
import com.cts.healthcare_appointment_system.dto.AvailabilityResponseDTO;
import com.cts.healthcare_appointment_system.dto.AvailabilitySlotGenerationDTO;
import com.cts.healthcare_appointment_system.dto.AvailabilityUpdateDTO;
import com.cts.healthcare_appointment_system.dto.PageResponseDTO;
import com.cts.healthcare_appointment_system.enums.AppointmentStatus;
import com.cts.healthcare_appointment_system.enums.UserRole;
import com.cts.healthcare_appointment_system.error.ApiException;
import com.cts.healthcare_appointment_system.models.Appointment;
import com.cts.healthcare_appointment_system.models.Availability;
import com.cts.healthcare_appointment_system.models.User;
import com.cts.healthcare_appointment_system.repositories.AppointmentRepository;
import com.cts.healthcare_appointment_system.repositories.AvailabilityRepository;
import com.cts.healthcare_appointment_system.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepo;
    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    // GET methods
    // Get all availabilities 
    public ResponseEntity<List<AvailabilityResponseDTO>> getAllAvailabilities(int doctorId, String namePrefix, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String isAvailable) {
        List<Availability> availabilities = availabilityRepo.findAll(Sort.by(Direction.ASC, "timeSlotStart"));

        availabilities = filterAvailabilities(doctorId, namePrefix, timeSlotStart, timeSlotEnd, isAvailable, availabilities);

        if (availabilities.isEmpty()) {
            throw new ApiException("No availabilities found", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(availabilities.stream().map(AvailabilityResponseDTO::from).toList());
    }

    public ResponseEntity<PageResponseDTO<AvailabilityResponseDTO>> getAvailabilitiesPage(int doctorId, String namePrefix, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String isAvailable, int page, int size, String sortBy, String sortDir) {
        validatePageRequest(page, size);

        List<Availability> availabilities = availabilityRepo.findAll();
        availabilities = filterAvailabilities(doctorId, namePrefix, timeSlotStart, timeSlotEnd, isAvailable, availabilities);
        availabilities = sortAvailabilities(availabilities, sortBy, sortDir);

        if (availabilities.isEmpty()) {
            throw new ApiException("No availabilities found", HttpStatus.BAD_REQUEST);
        }

        int totalElements = availabilities.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        if (fromIndex >= totalElements) {
            throw new ApiException("Requested page is out of range", HttpStatus.BAD_REQUEST);
        }

        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AvailabilityResponseDTO> content = availabilities.subList(fromIndex, toIndex).stream().map(AvailabilityResponseDTO::from).toList();

        PageResponseDTO<AvailabilityResponseDTO> response = new PageResponseDTO<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page == totalPages - 1);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get availabilities by id
    public ResponseEntity<AvailabilityResponseDTO> getAllAvailabilityById(int id) {
        Availability availability = availabilityRepo.findById(id).orElse(null);
        if (availability == null) {
            throw new ApiException("Availability not found with id: " + id, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(HttpStatus.OK).body(AvailabilityResponseDTO.from(availability));
    }

    // PUT methods
    @Transactional
    public ResponseEntity<AvailabilityResponseDTO> editAvailability(AvailabilityUpdateDTO dto) {
        int availabilityId = dto.getAvailabilityId();
        int doctorId = dto.getDoctorId();
        LocalDateTime timeSlotStart = dto.getTimeSlotStart();
        LocalDateTime timeSlotEnd = dto.getTimeSlotEnd();

        Availability availability = availabilityRepo.findById(availabilityId).orElse(null);

        // Check the availability is correct or not
        if (availability == null) {
            throw new ApiException("Availability not found with id: " + availabilityId, HttpStatus.BAD_REQUEST);
        }

        // Can't update past time slots
        if (availability.getTimeSlotEnd().isBefore(LocalDateTime.now())) {
            throw new ApiException("Can't update past availability slots", HttpStatus.BAD_REQUEST);
        }

        // Find the associated doctor (if any)
        User doctor = userRepo.findById(doctorId).orElse(null);

        // Check the doctor's correctness
        if (doctor == null || doctor.getRole() != UserRole.DOCTOR) {
            throw new ApiException("Invalid doctor id: " + doctorId, HttpStatus.BAD_REQUEST);
        }

        // Check if the correct doctorId is sent with the availability
        if (availability.getDoctor().getUserId() != doctorId) {
            throw new ApiException("Doctor with id: " + doctorId + " is not associated with availability with id: " + availabilityId, HttpStatus.BAD_REQUEST);
        }

        if (timeSlotStart.isAfter(timeSlotEnd)) {
            throw new ApiException("Invalid time slot details: " + timeSlotStart + " (timeSlotStart) is after " + timeSlotEnd + " (timeSlotEnd)", HttpStatus.BAD_REQUEST);
        }

        // Check if the time slot is valid (within 1 to 3 hrs)
        if (!checkForValidDuration(timeSlotStart, timeSlotEnd, 60L, 180L)) {
            throw new ApiException("Time slot must be at minimum " + 1 + " hr., and maximum " + 3 + " hrs.", HttpStatus.BAD_REQUEST);
        }

        validateNoAvailabilityOverlap(doctorId, timeSlotStart, timeSlotEnd, availabilityId);

        // When the availability slot changes, the associated appointment (if any) must also change
        List<Appointment> appointments = appointmentRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(doctorId, availability.getTimeSlotStart(), availability.getTimeSlotEnd());

        // Change the associated appointment slots
        appointments.forEach(ap -> {

            // Change time slot and send rescheduled email
            if (ap.getStatus() == AppointmentStatus.BOOKED) {
                ap.setTimeSlotStart(timeSlotStart);
                ap.setTimeSlotEnd(timeSlotEnd);
                appointmentRepo.save(ap);
                notificationService.sendRescheduledEmail(ap);
            }
        });

        // Finally, update the availability timeslot
        availability.setTimeSlotStart(timeSlotStart);
        availability.setTimeSlotEnd(timeSlotEnd);

        // Save the edited availability
        availabilityRepo.save(availability);

        log.info("Edited an availability with id: {} from {} to {} for doctor with id: {}", dto.getAvailabilityId(), dto.getTimeSlotStart(), dto.getTimeSlotEnd(), dto.getDoctorId());

        return ResponseEntity.status(HttpStatus.OK).body(AvailabilityResponseDTO.from(availability));
    }

    // POST methods
    // Save a new availability and associate with a doctor
    @Transactional
    public ResponseEntity<AvailabilityResponseDTO> saveAvailability(AvailabilityDTO dto) {
        int doctorId = dto.getDoctorId();
        LocalDateTime timeSlotStart = dto.getTimeSlotStart();
        LocalDateTime timeSlotEnd = dto.getTimeSlotEnd();

        // Find the associated doctor (if any)
        User doctor = userRepo.findById(doctorId).orElse(null);
        // Check the doctor's correctness
        if (doctor == null || doctor.getRole() != UserRole.DOCTOR) {
            throw new ApiException("Invalid doctor id: " + doctorId, HttpStatus.BAD_REQUEST);
        }

        if (timeSlotStart.isAfter(timeSlotEnd)) {
            throw new ApiException("Invalid time slot details: " + timeSlotStart + " (timeSlotStart) is after " + timeSlotEnd + " (timeSlotEnd)", HttpStatus.BAD_REQUEST);
        }

        // Check if the time slot is valid (within 1 to 3 hrs)
        if (!checkForValidDuration(timeSlotStart, timeSlotEnd, 60, 180)) {
            throw new ApiException("Time slot must be at minimum " + 1 + " hrs, and maximum " + 3 + " hrs.", HttpStatus.BAD_REQUEST);
        }

        validateNoAvailabilityOverlap(doctorId, timeSlotStart, timeSlotEnd, null);

        Availability newAvailability = new Availability();
        newAvailability.setTimeSlotStart(timeSlotStart);
        newAvailability.setTimeSlotEnd(timeSlotEnd);

        // Associating the new availability with the doctor
        doctor.addAvailability(newAvailability);

        // Save both entities
        availabilityRepo.save(newAvailability);
        userRepo.save(doctor);

        log.info("Created an availability slot from {} to {} for doctor with id: {}", dto.getTimeSlotStart(), dto.getTimeSlotEnd(), dto.getDoctorId());

        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityResponseDTO.from(newAvailability));
    }

    @Transactional
    public ResponseEntity<List<AvailabilityResponseDTO>> generateAvailabilitySlots(AvailabilitySlotGenerationDTO dto) {
        int doctorId = dto.getDoctorId();
        LocalDateTime windowStart = dto.getWindowStart();
        LocalDateTime windowEnd = dto.getWindowEnd();
        int slotDurationMinutes = dto.getSlotDurationMinutes();
        int bufferMinutes = dto.getBufferMinutes() == null ? 0 : dto.getBufferMinutes();

        User doctor = userRepo.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getRole() != UserRole.DOCTOR) {
            throw new ApiException("Invalid doctor id: " + doctorId, HttpStatus.BAD_REQUEST);
        }
        if (!windowStart.isBefore(windowEnd)) {
            throw new ApiException("Window start must be before window end", HttpStatus.BAD_REQUEST);
        }
        if (Duration.between(windowStart, windowEnd).toMinutes() < slotDurationMinutes) {
            throw new ApiException("Window must be large enough for at least one slot", HttpStatus.BAD_REQUEST);
        }

        List<Availability> generatedSlots = new ArrayList<>();
        LocalDateTime slotStart = windowStart;
        while (!slotStart.plusMinutes(slotDurationMinutes).isAfter(windowEnd)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);

            if (!availabilityRepo.existsDoctorAvailabilityOverlap(doctorId, slotStart, slotEnd, null)) {
                Availability availability = new Availability();
                availability.setTimeSlotStart(slotStart);
                availability.setTimeSlotEnd(slotEnd);
                doctor.addAvailability(availability);
                generatedSlots.add(availability);
            }

            slotStart = slotEnd.plusMinutes(bufferMinutes);
        }

        if (generatedSlots.isEmpty()) {
            throw new ApiException("No slots generated. The requested window overlaps existing availability.", HttpStatus.BAD_REQUEST);
        }

        availabilityRepo.saveAll(generatedSlots);
        userRepo.save(doctor);

        log.info("Generated {} availability slots for doctor with id: {}", generatedSlots.size(), doctorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(generatedSlots.stream().map(AvailabilityResponseDTO::from).toList());
    }

    // DELETE methods
    // Find an availability with the given id and delete it
    @Transactional
    public ResponseEntity<AvailabilityResponseDTO> deleteAvailabilityByid(int id) {
        // Find the availability with given id
        Availability delAvailability = availabilityRepo.findById(id).orElse(null);
        if (delAvailability == null) {
            throw new ApiException("Availability not found with id: " + id, HttpStatus.BAD_REQUEST);
        }

        // Cancel the associated appointment (if any)
        if (!delAvailability.isAvailable()) {
            List<Appointment> appointments = appointmentRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(delAvailability.getDoctor().getUserId(), delAvailability.getTimeSlotStart(), delAvailability.getTimeSlotEnd());

            // If the time slot is not in the past and can't fetch associated appointments
            if (appointments.isEmpty() && delAvailability.getTimeSlotEnd().isAfter(LocalDateTime.now())) {
                throw new ApiException("Can't fetch the associated appointments", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Cancel the associated appointment slots and send cancellation mail
            appointments.forEach(ap -> {
                if (ap.getStatus() == AppointmentStatus.BOOKED) {
                    // Only when no consultation is given delete the associated appointment
                    if(ap.getConsultation() == null){
                        ap.cancel();
                        appointmentRepo.save(ap);
                        log.info("Cancelled an appointment with id: {}", ap.getAppointmentId());
                        // Send cancellation mail
                        notificationService.sendCancellationEmail(ap);
                    }else{
                        throw new ApiException("Please remove the consultation of the associated appointment, before deleting the slot.", HttpStatus.BAD_REQUEST);
                    }
                }
            });
        }

        log.info("Deleted an availability slot with id: {} from {} to {} for doctor with id: {}", id, delAvailability.getTimeSlotStart(), delAvailability.getTimeSlotEnd(), delAvailability.getDoctor().getUserId());
        AvailabilityResponseDTO response = AvailabilityResponseDTO.from(delAvailability);
        
        // Breaking the associativity with the doctor
        delAvailability.getDoctor().removeAvailability(delAvailability);

        // Deleting the availability
        availabilityRepo.delete(delAvailability);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Utility methods
    public boolean checkForValidDuration(LocalDateTime start, LocalDateTime end, long min, long max) {
        Duration duration = Duration.between(start, end);
        return duration.toMinutes() <= max && duration.toMinutes() >= min;
    }

    private void validateNoAvailabilityOverlap(int doctorId, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, Integer excludedAvailabilityId) {
        if (availabilityRepo.existsDoctorAvailabilityOverlap(doctorId, timeSlotStart, timeSlotEnd, excludedAvailabilityId)) {
            throw new ApiException("Availability slot overlaps", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Availability> filterAvailabilities(int doctorId, String namePrefix, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String isAvailable, List<Availability> availabilities) {
        if (doctorId != 0) {
            availabilities = availabilities.stream().filter(a -> a.getDoctor().getUserId() == doctorId).toList();
        }

        if (namePrefix != null) {
            if (!namePrefix.trim().equals("")) {
                availabilities = availabilities.stream().filter(a -> a.getDoctor().getName().toLowerCase().startsWith(namePrefix.trim().toLowerCase())).toList();
            }
        }

        if (isAvailable != null) {
            if (isAvailable.equalsIgnoreCase("true")) {
                availabilities = availabilities.stream().filter(a -> a.isAvailable()).toList();
            } else if (isAvailable.equalsIgnoreCase("false")) {
                availabilities = availabilities.stream().filter(a -> !a.isAvailable()).toList();
            } else {
                throw new ApiException("Invalid availability status: " + isAvailable, HttpStatus.BAD_REQUEST);
            }
        }

        if (timeSlotStart != null) {
            availabilities = availabilities.stream().filter(a -> a.getTimeSlotStart().isAfter(timeSlotStart) || a.getTimeSlotStart().isEqual(timeSlotStart)).toList();
        }

        if (timeSlotEnd != null) {
            availabilities = availabilities.stream().filter(a -> a.getTimeSlotEnd().isBefore(timeSlotEnd) || a.getTimeSlotEnd().isEqual(timeSlotEnd)).toList();
        }

        return availabilities;
    }

    private List<Availability> sortAvailabilities(List<Availability> availabilities, String sortBy, String sortDir) {
        Comparator<Availability> comparator = switch (sortBy) {
            case "timeSlotStart" -> Comparator.comparing(Availability::getTimeSlotStart);
            case "timeSlotEnd" -> Comparator.comparing(Availability::getTimeSlotEnd);
            case "available" -> Comparator.comparing(Availability::isAvailable);
            case "availabilityId" -> Comparator.comparing(Availability::getAvailabilityId);
            default -> throw new ApiException("Invalid availability sort field: " + sortBy, HttpStatus.BAD_REQUEST);
        };

        if (sortDir.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        } else if (!sortDir.equalsIgnoreCase("asc")) {
            throw new ApiException("Invalid sort direction: " + sortDir, HttpStatus.BAD_REQUEST);
        }

        return availabilities.stream().sorted(comparator).toList();
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new ApiException("Page number cannot be negative", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > 100) {
            throw new ApiException("Page size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }
    }

}
