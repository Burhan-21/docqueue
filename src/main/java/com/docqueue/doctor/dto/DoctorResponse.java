package com.docqueue.doctor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorResponse {
    private Long       id;
    private String     name;
    private String     email;
    private String     specialization;
    private String     qualification;
    private int        avgConsultMin;
    private String     bio;
    private BigDecimal consultationFee;
    private String     clinicName;
    private String     clinicCity;
    private Long       clinicId;
    private boolean    available;
}
