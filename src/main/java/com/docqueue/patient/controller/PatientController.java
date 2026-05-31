package com.docqueue.patient.controller;

import com.docqueue.common.response.ApiResponse;
import com.docqueue.patient.dto.PatientProfileRequest;
import com.docqueue.patient.dto.PatientProfileResponse;
import com.docqueue.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient profile management")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my patient profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(patientService.getProfile(auth.getName())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update my patient profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updateProfile(
            @Valid @RequestBody PatientProfileRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated", patientService.updateProfile(auth.getName(), request)));
    }
}
