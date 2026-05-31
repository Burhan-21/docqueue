package com.docqueue.doctor.controller;

import com.docqueue.common.response.ApiResponse;
import com.docqueue.common.response.PagedResponse;
import com.docqueue.doctor.dto.AvailabilitySlotDto;
import com.docqueue.doctor.dto.DoctorRequest;
import com.docqueue.doctor.dto.DoctorResponse;
import com.docqueue.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Doctor REST controller.
 *
 * Public endpoints (no auth):  GET /api/v1/doctors, /{id}, /{id}/slots
 * Doctor endpoints:            PUT /api/v1/doctors/{id}/availability
 * Admin endpoints:             POST, PUT /{id}, DELETE /{id}
 */
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor search, profile, and availability management")
public class DoctorController {

    private final DoctorService doctorService;

    // ===== Public: Search & Profile =====

    @GetMapping
    @Operation(summary = "Search doctors by specialization (paginated, public)")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorResponse>>> search(
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<DoctorResponse> result = doctorService.search(
                specialization,
                PageRequest.of(page, size, Sort.by("specialization").ascending()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor profile by ID (public)")
    public ResponseEntity<ApiResponse<DoctorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getById(id)));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Get available time slots for a doctor on a given date (public)")
    public ResponseEntity<ApiResponse<List<String>>> getSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAvailableSlots(id, date)));
    }

    // ===== Doctor: Manage Availability =====

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get doctor's weekly availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDto>>> getAvailability(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAvailability(id)));
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Set doctor's weekly availability schedule")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDto>>> setAvailability(
            @PathVariable Long id,
            @Valid @RequestBody List<AvailabilitySlotDto> slots) {
        return ResponseEntity.ok(ApiResponse.success(
                "Availability updated", doctorService.setAvailability(id, slots)));
    }

    // ===== Admin: Doctor CRUD =====

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a doctor profile (Admin only)")
    public ResponseEntity<ApiResponse<DoctorResponse>> create(
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor created", doctorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update a doctor profile (Admin only)")
    public ResponseEntity<ApiResponse<DoctorResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Doctor updated", doctorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Soft-delete a doctor (Admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor removed", null));
    }
}
