package com.cts.healthcare_appointment_system.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cts.healthcare_appointment_system.dto.AppointmentDTO;
import com.cts.healthcare_appointment_system.dto.AppointmentRescheduleDTO;
import com.cts.healthcare_appointment_system.dto.AvailabilitySlotGenerationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDoctorCannotBookAppointment() throws Exception {
        AppointmentDTO dto = new AppointmentDTO(
                1,
                2,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30));

        mockMvc.perform(post("/appointments")
                .with(user("doctor@example.com").authorities(() -> "DOCTOR"))
                .content(objectMapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoctorCannotRescheduleAppointment() throws Exception {
        AppointmentRescheduleDTO dto = new AppointmentRescheduleDTO(
                1,
                2,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30));

        mockMvc.perform(put("/appointments/reschedule")
                .with(user("doctor@example.com").authorities(() -> "DOCTOR"))
                .content(objectMapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPatientCannotGenerateAvailabilitySlots() throws Exception {
        AvailabilitySlotGenerationDTO dto = new AvailabilitySlotGenerationDTO(
                2,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                30,
                0);

        mockMvc.perform(post("/availabilities/generate")
                .with(user("patient@example.com").authorities(() -> "PATIENT"))
                .content(objectMapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
