package com.cts.healthcare_appointment_system.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.healthcare_appointment_system.dto.AppointmentDTO;
import com.cts.healthcare_appointment_system.dto.AppointmentRescheduleDTO;
import com.cts.healthcare_appointment_system.dto.AppointmentResponseDTO;
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
public class AppointmentService {

    private AppointmentRepository appointmentRepo;
    private AvailabilityRepository availabilityRepo;
    private UserRepository userRepo;
    private NotificationService notificationService;
    private AuditLogService auditLogService;

    // GET methods
    // Get all appointments
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointments(int patientId, int doctorId, String patientName, String doctorName, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String status) {
        List<Appointment> appointments = appointmentRepo.findAll(Sort.by(Direction.DESC, "timeSlotStart"));

        appointments = filterAppointments(patientId, doctorId, patientName, doctorName, timeSlotStart, timeSlotEnd, status, appointments);

        if (appointments.isEmpty()) {
            throw new ApiException("No appointments found", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.OK).body(appointments.stream().map(AppointmentResponseDTO::from).toList());
    }

    public ResponseEntity<PageResponseDTO<AppointmentResponseDTO>> getAppointmentsPage(int patientId, int doctorId, String patientName, String doctorName, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String status, int page, int size, String sortBy, String sortDir) {
        validatePageRequest(page, size);

        List<Appointment> appointments = appointmentRepo.findAll();
        appointments = filterAppointments(patientId, doctorId, patientName, doctorName, timeSlotStart, timeSlotEnd, status, appointments);
        appointments = sortAppointments(appointments, sortBy, sortDir);

        if (appointments.isEmpty()) {
            throw new ApiException("No appointments found", HttpStatus.BAD_REQUEST);
        }

        int totalElements = appointments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        if (fromIndex >= totalElements) {
            throw new ApiException("Requested page is out of range", HttpStatus.BAD_REQUEST);
        }

        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AppointmentResponseDTO> content = appointments.subList(fromIndex, toIndex).stream().map(AppointmentResponseDTO::from).toList();

        PageResponseDTO<AppointmentResponseDTO> response = new PageResponseDTO<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page == totalPages - 1);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Get appointment by id
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(int id) {
        Appointment appointment = appointmentRepo.findById(id).orElse(null);
        if (appointment == null) {
            throw new ApiException("No appointment found with id: " + id, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(HttpStatus.OK).body(AppointmentResponseDTO.from(appointment));
    }

    // POST methods
    // Save an appointment
    @Transactional
    public ResponseEntity<AppointmentResponseDTO> saveAppointment(AppointmentDTO dto) {
        int patientId = dto.getPatientId();
        int doctorId = dto.getDoctorId();
        LocalDateTime timeSlotStart = dto.getTimeSlotStart();
        LocalDateTime timeSlotEnd = dto.getTimeSlotEnd();

        User patient = userRepo.findById(patientId).orElse(null);
        User doctor = userRepo.findById(doctorId).orElse(null);

        Availability availability = availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(doctorId, timeSlotStart, timeSlotEnd).orElse(null);

        if (patient == null || patient.getRole() != UserRole.PATIENT) {
            throw new ApiException("Invalid patient id: " + patientId, HttpStatus.BAD_REQUEST);
        }

        if (doctor == null || doctor.getRole() != UserRole.DOCTOR) {
            throw new ApiException("Invalid doctor id: " + doctorId, HttpStatus.BAD_REQUEST);
        }

        if (availability == null) {
            throw new ApiException("Invalid availability time slot details", HttpStatus.BAD_REQUEST);
        }
        if (!availability.isAvailable()) {
            throw new ApiException("Sorry, the slot is not available", HttpStatus.BAD_REQUEST);
        }

        validateTimeRange(timeSlotStart, timeSlotEnd);
        validateNoBookedOverlap(patientId, doctorId, timeSlotStart, timeSlotEnd, null);

        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setTimeSlotStart(timeSlotStart);
        appointment.setTimeSlotEnd(timeSlotEnd);
        appointment.book();

        availability.setAvailable(false);

        doctor.getDoctorAppointments().add(appointment);
        patient.getPatientAppointments().add(appointment);

        userRepo.save(patient);
        userRepo.save(doctor);

        availabilityRepo.save(availability);

        appointmentRepo.save(appointment);

        log.info("Created new appointment for doctor with id: {} and patient with id: {}", doctor.getUserId(), patient.getUserId());

        // Send appointment booked email
        notificationService.sendBookedEmail(appointment);
        auditLogService.record(patient.getUserId(), "APPOINTMENT_BOOKED", "APPOINTMENT", appointment.getAppointmentId(), "Appointment booked with doctor id: " + doctor.getUserId());

        return ResponseEntity.status(HttpStatus.OK).body(AppointmentResponseDTO.from(appointment));
    }

    // PUT methods
    // Cancel an appointment
    @Transactional
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(int id) {
        Appointment appointment = appointmentRepo.findById(id).orElse(null);
        if (appointment == null) {
            throw new ApiException("Invalid appointment with id: " + id, HttpStatus.BAD_REQUEST);
        }

        if(appointment.getConsultation() != null){
            throw new ApiException("Can't cancel an appointment after consultation is given.", HttpStatus.BAD_REQUEST);
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.cancel();
        } else {
            throw new ApiException("Can't cancel a completed appointment", HttpStatus.BAD_REQUEST);
        }
        int doctorId = appointment.getDoctor().getUserId();
        LocalDateTime timeSlotStart = appointment.getTimeSlotStart();
        LocalDateTime timeSlotEnd = appointment.getTimeSlotEnd();

        Availability availability = availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(doctorId, timeSlotStart, timeSlotEnd).orElse(null);

        // Make the availability slot available, if it is cancelled before end
        if (timeSlotEnd.isAfter(LocalDateTime.now())) {
            if(availability != null){
                availability.setAvailable(true);   // Mark the slot available
                availabilityRepo.save(availability);
            }
        }

        appointmentRepo.save(appointment);

        log.info("Cancelled an appointment with id: {}", appointment.getAppointmentId());

        // Send appointment cancellation email
        notificationService.sendCancellationEmail(appointment);
        auditLogService.record(appointment.getPatient() == null ? null : appointment.getPatient().getUserId(), "APPOINTMENT_CANCELLED", "APPOINTMENT", appointment.getAppointmentId(), "Appointment was cancelled");

        return ResponseEntity.status(HttpStatus.OK).body(AppointmentResponseDTO.from(appointment));
    }

    // Reschedule an appointment to another available slot
    @Transactional
    public ResponseEntity<AppointmentResponseDTO> rescheduleAppointment(AppointmentRescheduleDTO dto) {
        int appointmentId = dto.getAppointmentId();
        int doctorId = dto.getDoctorId();
        LocalDateTime newTimeSlotStart = dto.getTimeSlotStart();
        LocalDateTime newTimeSlotEnd = dto.getTimeSlotEnd();

        validateTimeRange(newTimeSlotStart, newTimeSlotEnd);

        Appointment appointment = appointmentRepo.findById(appointmentId).orElse(null);
        if (appointment == null) {
            throw new ApiException("Invalid appointment with id: " + appointmentId, HttpStatus.BAD_REQUEST);
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ApiException("Only booked appointments can be rescheduled", HttpStatus.BAD_REQUEST);
        }
        if (appointment.getConsultation() != null) {
            throw new ApiException("Can't reschedule an appointment after consultation is given.", HttpStatus.BAD_REQUEST);
        }
        if (appointment.getTimeSlotStart().isBefore(LocalDateTime.now())) {
            throw new ApiException("Can't reschedule an appointment after it has started", HttpStatus.BAD_REQUEST);
        }

        User doctor = userRepo.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getRole() != UserRole.DOCTOR) {
            throw new ApiException("Invalid doctor id: " + doctorId, HttpStatus.BAD_REQUEST);
        }

        Availability newAvailability = availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(doctorId, newTimeSlotStart, newTimeSlotEnd).orElse(null);
        if (newAvailability == null) {
            throw new ApiException("Invalid availability time slot details", HttpStatus.BAD_REQUEST);
        }
        if (!newAvailability.isAvailable()) {
            throw new ApiException("Sorry, the slot is not available", HttpStatus.BAD_REQUEST);
        }

        int patientId = appointment.getPatient().getUserId();
        validateNoBookedOverlap(patientId, doctorId, newTimeSlotStart, newTimeSlotEnd, appointmentId);

        Availability oldAvailability = availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(
                appointment.getDoctor().getUserId(),
                appointment.getTimeSlotStart(),
                appointment.getTimeSlotEnd()).orElse(null);
        if (oldAvailability != null && oldAvailability.getTimeSlotEnd().isAfter(LocalDateTime.now())) {
            oldAvailability.setAvailable(true);
            availabilityRepo.save(oldAvailability);
        }

        newAvailability.setAvailable(false);
        availabilityRepo.save(newAvailability);

        appointment.setDoctor(doctor);
        appointment.setTimeSlotStart(newTimeSlotStart);
        appointment.setTimeSlotEnd(newTimeSlotEnd);
        appointmentRepo.save(appointment);

        log.info("Rescheduled appointment with id: {} to doctor id: {} from {} to {}", appointmentId, doctorId, newTimeSlotStart, newTimeSlotEnd);
        notificationService.sendRescheduledEmail(appointment);
        auditLogService.record(patientId, "APPOINTMENT_RESCHEDULED", "APPOINTMENT", appointment.getAppointmentId(), "Appointment rescheduled to doctor id: " + doctorId);

        return ResponseEntity.status(HttpStatus.OK).body(AppointmentResponseDTO.from(appointment));
    }

    // Complete an appointment
    @Transactional
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(int id) {
        Appointment appointment = appointmentRepo.findById(id).orElse(null);

        if (appointment == null) {
            throw new ApiException("Invalid appointment with id: " + id, HttpStatus.BAD_REQUEST);
        }

        if(appointment.getStatus() == AppointmentStatus.CANCELLED){
            throw new ApiException("Can't mark a cancelled appointment as completed", HttpStatus.BAD_REQUEST);
        }

        int doctorId = appointment.getDoctor().getUserId();
        LocalDateTime timeSlotStart = appointment.getTimeSlotStart();
        LocalDateTime timeSlotEnd = appointment.getTimeSlotEnd();

        Availability availability = availabilityRepo.findByDoctorUserIdAndTimeSlotStartAndTimeSlotEnd(doctorId, timeSlotStart, timeSlotEnd).orElse(null);

        // If the appointment already started?
        if (appointment.getTimeSlotStart().isBefore(LocalDateTime.now())) {
            appointment.complete();   // Mark as complete
            if(availability != null){
                availability.setAvailable(false);   // Make the slot unavailable
                availabilityRepo.save(availability);
            }
        } else {
            throw new ApiException("Can't mark as complete an appointment before it has started", HttpStatus.BAD_REQUEST);
        }

        appointmentRepo.save(appointment);

        log.info("Completed an appointment with id: {}", appointment.getAppointmentId());

        // Send appointment completion email
        notificationService.sendCompletionEmail(appointment);
        auditLogService.record(appointment.getDoctor() == null ? null : appointment.getDoctor().getUserId(), "APPOINTMENT_COMPLETED", "APPOINTMENT", appointment.getAppointmentId(), "Appointment was completed");

        return ResponseEntity.status(HttpStatus.OK).body(AppointmentResponseDTO.from(appointment));
    }

    private void validateTimeRange(LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd) {
        if (!timeSlotStart.isBefore(timeSlotEnd)) {
            throw new ApiException("Time slot start must be before time slot end", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateNoBookedOverlap(int patientId, int doctorId, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, Integer excludedAppointmentId) {
        if (appointmentRepo.existsBookedPatientOverlap(patientId, timeSlotStart, timeSlotEnd, excludedAppointmentId)) {
            throw new ApiException("Patient already has a booked appointment in this time slot", HttpStatus.BAD_REQUEST);
        }
        if (appointmentRepo.existsBookedDoctorOverlap(doctorId, timeSlotStart, timeSlotEnd, excludedAppointmentId)) {
            throw new ApiException("Doctor already has a booked appointment in this time slot", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Appointment> filterAppointments(int patientId, int doctorId, String patientName, String doctorName, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String status, List<Appointment> appointments) {
        if(patientId != 0){
            // As, the associated patient or doctor can be NULL (may be they have deleted their account), checking not NULL to avoid NULL pointer exception
            appointments = appointments.stream().filter(a -> a.getPatient() != null).filter(a -> a.getPatient().getUserId() == patientId).toList();
        }
        if(doctorId != 0){
            appointments = appointments.stream().filter(a -> a.getDoctor() != null).filter(a -> a.getDoctor().getUserId() == doctorId).toList();
        }

        if(patientName != null){
            if (!patientName.trim().equals("")) {
                appointments = appointments.stream().filter(a -> a.getPatient() != null).filter(a -> a.getPatient().getName().toLowerCase().startsWith(patientName.trim().toLowerCase())).toList();
            }
        }

        if(doctorName != null){
            if (!doctorName.trim().equals("")) {
                appointments = appointments.stream().filter(a -> a.getDoctor() != null).filter(a -> a.getDoctor().getName().toLowerCase().startsWith(doctorName.trim().toLowerCase())).toList();
            }
        }

        if (timeSlotStart != null) {
            appointments = appointments.stream().filter(a -> a.getTimeSlotStart().isAfter(timeSlotStart) || a.getTimeSlotStart().isEqual(timeSlotStart)).toList();
        }

        if (timeSlotEnd != null) {
            appointments = appointments.stream().filter(a -> a.getTimeSlotEnd().isBefore(timeSlotEnd) || a.getTimeSlotEnd().isEqual(timeSlotEnd)).toList();
        }

        if (status != null) {
            if (status.equalsIgnoreCase("cancelled")) {
                appointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).toList();
            } else if (status.equalsIgnoreCase("booked")) {
                appointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.BOOKED).toList();
            } else if (status.equalsIgnoreCase("completed")) {
                appointments = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();
            } else {
                throw new ApiException("Invalid status provided: " + status, HttpStatus.BAD_REQUEST);
            }
        }
        return appointments;
    }

    private List<Appointment> sortAppointments(List<Appointment> appointments, String sortBy, String sortDir) {
        Comparator<Appointment> comparator = switch (sortBy) {
            case "timeSlotStart" -> Comparator.comparing(Appointment::getTimeSlotStart);
            case "timeSlotEnd" -> Comparator.comparing(Appointment::getTimeSlotEnd);
            case "status" -> Comparator.comparing(a -> a.getStatus().name());
            case "appointmentId" -> Comparator.comparing(Appointment::getAppointmentId);
            default -> throw new ApiException("Invalid appointment sort field: " + sortBy, HttpStatus.BAD_REQUEST);
        };

        if (sortDir.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        } else if (!sortDir.equalsIgnoreCase("asc")) {
            throw new ApiException("Invalid sort direction: " + sortDir, HttpStatus.BAD_REQUEST);
        }

        return appointments.stream().sorted(comparator).toList();
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

