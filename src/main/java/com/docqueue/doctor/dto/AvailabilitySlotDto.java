package com.docqueue.doctor.dto;

import com.docqueue.doctor.entity.DoctorAvailability.DayOfWeek;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AvailabilitySlotDto {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Min(1) @Max(100)
    private int maxSlots = 20;

    private boolean isActive = true;
}
