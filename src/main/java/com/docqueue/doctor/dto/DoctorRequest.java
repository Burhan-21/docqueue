package com.docqueue.doctor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoctorRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Clinic ID is required")
    private Long clinicId;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100)
    private String specialization;

    @Size(max = 200)
    private String qualification;

    @Min(value = 5, message = "Min consult time is 5 minutes")
    @Max(value = 120, message = "Max consult time is 120 minutes")
    private int avgConsultMin = 15;

    @Size(max = 2000)
    private String bio;

    @DecimalMin(value = "0.0")
    private BigDecimal consultationFee;
}
