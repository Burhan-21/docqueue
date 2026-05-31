package com.docqueue.appointment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentResponse {
    private Long          id;
    private String        doctorName;
    private String        doctorSpecialization;
    private String        clinicName;
    private String        patientName;   // populated for doctor/admin views
    private LocalDateTime scheduledAt;
    private String        status;
    private int           tokenNumber;
    private String        notes;
    private LocalDateTime createdAt;
    private QueuePositionDto queuePosition; // populated if status = CONFIRMED/WAITING
}
