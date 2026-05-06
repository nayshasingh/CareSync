package com.cts.healthcare_appointment_system.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotGenerationDTO {

    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be positive")
    private Integer doctorId;

    @NotNull(message = "Window start cannot be null")
    @Future(message = "Window start must be in the future")
    private LocalDateTime windowStart;

    @NotNull(message = "Window end cannot be null")
    @Future(message = "Window end must be in the future")
    private LocalDateTime windowEnd;

    @NotNull(message = "Slot duration is required")
    @Min(value = 15, message = "Slot duration must be at least 15 minutes")
    @Max(value = 180, message = "Slot duration cannot exceed 180 minutes")
    private Integer slotDurationMinutes;

    @Min(value = 0, message = "Buffer duration cannot be negative")
    @Max(value = 120, message = "Buffer duration cannot exceed 120 minutes")
    private Integer bufferMinutes = 0;
}
