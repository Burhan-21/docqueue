package com.docqueue.appointment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookAppointmentRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment must be in the future")
    private LocalDateTime scheduledAt;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
