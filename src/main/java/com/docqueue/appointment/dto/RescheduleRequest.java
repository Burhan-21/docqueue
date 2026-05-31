package com.docqueue.appointment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleRequest {

    @NotNull(message = "New appointment time is required")
    @Future(message = "Rescheduled time must be in the future")
    private LocalDateTime newScheduledAt;

    @Size(max = 200)
    private String reason;
}
