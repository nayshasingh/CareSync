package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponseDTO {
    private LocalDateTime timestamp;
    private String error;
    private String message;
    private int statusCode;
    private String path;
    private String method;
    private Map<String, String> fieldErrors;
}
