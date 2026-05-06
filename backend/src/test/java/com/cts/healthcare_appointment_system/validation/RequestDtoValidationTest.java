package com.cts.healthcare_appointment_system.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cts.healthcare_appointment_system.dto.AppointmentDTO;
import com.cts.healthcare_appointment_system.dto.ConsultationDTO;
import com.cts.healthcare_appointment_system.dto.UserLoginDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class RequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testAppointmentRejectsNonPositiveIds() {
        AppointmentDTO dto = new AppointmentDTO(
                0,
                -1,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2));

        Set<ConstraintViolation<AppointmentDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void testConsultationRejectsBlankText() {
        ConsultationDTO dto = new ConsultationDTO(1, "   ", "   ");

        Set<ConstraintViolation<ConsultationDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void testLoginRejectsBlankPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("patient@example.com");
        dto.setPassword("   ");

        Set<ConstraintViolation<UserLoginDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }
}
