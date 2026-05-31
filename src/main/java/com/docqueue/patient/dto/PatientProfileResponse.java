package com.docqueue.patient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientProfileResponse {
    private Long      id;
    private String    name;
    private String    email;
    private String    phone;
    private LocalDate dob;
    private String    gender;
    private String    bloodGroup;
}
