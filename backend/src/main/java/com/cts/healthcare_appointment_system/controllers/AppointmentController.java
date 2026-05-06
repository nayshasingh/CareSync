package com.cts.healthcare_appointment_system.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.healthcare_appointment_system.dto.AppointmentDTO;
import com.cts.healthcare_appointment_system.dto.AppointmentRescheduleDTO;
import com.cts.healthcare_appointment_system.dto.AppointmentResponseDTO;
import com.cts.healthcare_appointment_system.dto.PageResponseDTO;
import com.cts.healthcare_appointment_system.services.AppointmentService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/appointments")
public class AppointmentController {
	
	private AppointmentService appointmentService;
	
	//Retrieve all appointment details (sorted by time_slot_start)
	@GetMapping
	public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointments(
			@RequestParam(defaultValue = "0") int patientId,
			@RequestParam(defaultValue = "0") int doctorId,
			@RequestParam(required = false) String patientName,
			@RequestParam(required = false) String doctorName,
			@RequestParam(required = false) LocalDateTime timeSlotStart,
			@RequestParam(required = false) LocalDateTime timeSlotEnd,
			@RequestParam(required = false) String status
			){
		return appointmentService.getAllAppointments(patientId, doctorId, patientName, doctorName, timeSlotStart, timeSlotEnd, status);
	}

	@GetMapping("/page")
	public ResponseEntity<PageResponseDTO<AppointmentResponseDTO>> getAppointmentsPage(
			@RequestParam(defaultValue = "0") int patientId,
			@RequestParam(defaultValue = "0") int doctorId,
			@RequestParam(required = false) String patientName,
			@RequestParam(required = false) String doctorName,
			@RequestParam(required = false) LocalDateTime timeSlotStart,
			@RequestParam(required = false) LocalDateTime timeSlotEnd,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "timeSlotStart") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir
			){
		return appointmentService.getAppointmentsPage(patientId, doctorId, patientName, doctorName, timeSlotStart, timeSlotEnd, status, page, size, sortBy, sortDir);
	}
	
	//Retrieve a specific appointment by id
	@GetMapping("/{id}")
	public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable int id){
		return appointmentService.getAppointmentById(id);
	}
	
	//Create a new appointment
	@PostMapping
	public ResponseEntity<AppointmentResponseDTO> saveAppointment(@Valid @RequestBody AppointmentDTO dto){
		return appointmentService.saveAppointment(dto);
	}
	
	//Cancel an appointment by id
	@PutMapping("/cancel/{id}")
	public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@PathVariable int id){
		return appointmentService.cancelAppointment(id);
	}

	//Reschedule an appointment to another available slot
	@PutMapping("/reschedule")
	public ResponseEntity<AppointmentResponseDTO> rescheduleAppointment(@Valid @RequestBody AppointmentRescheduleDTO dto) {
		return appointmentService.rescheduleAppointment(dto);
	}
	
	//Mark as completed an appointment
	@PutMapping("/complete/{id}")
	public ResponseEntity<AppointmentResponseDTO> completeAppointment(@PathVariable int id){
		return appointmentService.completeAppointment(id);
	}
}
