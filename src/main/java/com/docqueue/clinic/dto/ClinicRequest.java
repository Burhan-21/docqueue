package com.docqueue.clinic.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ClinicRequest {

    @NotBlank(message = "Clinic name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String address;

    @Pattern(regexp = "^[6-9]\\d{9}$|^[0-9]{10,12}$", message = "Invalid phone number")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;
}
