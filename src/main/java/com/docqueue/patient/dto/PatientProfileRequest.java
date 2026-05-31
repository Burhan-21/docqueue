package com.docqueue.patient.dto;

import com.docqueue.patient.entity.Patient.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileRequest {

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private Gender gender;

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood group")
    private String bloodGroup;
}
