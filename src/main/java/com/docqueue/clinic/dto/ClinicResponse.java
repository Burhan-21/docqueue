package com.docqueue.clinic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClinicResponse {
    private Long    id;
    private String  name;
    private String  address;
    private String  phone;
    private String  email;
    private String  city;
    private String  state;
    private boolean isActive;
}
