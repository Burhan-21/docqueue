package com.docqueue.clinic.controller;

import com.docqueue.clinic.dto.ClinicRequest;
import com.docqueue.clinic.dto.ClinicResponse;
import com.docqueue.clinic.service.ClinicService;
import com.docqueue.common.response.ApiResponse;
import com.docqueue.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management (Admin only write operations)")
public class ClinicController {

    private final ClinicService clinicService;

    @GetMapping
    @Operation(summary = "List all active clinics (public)")
    public ResponseEntity<ApiResponse<PagedResponse<ClinicResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                clinicService.listAll(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinic by ID (public)")
    public ResponseEntity<ApiResponse<ClinicResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(clinicService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a clinic (Admin only)")
    public ResponseEntity<ApiResponse<ClinicResponse>> create(
            @Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clinic created", clinicService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update clinic details (Admin only)")
    public ResponseEntity<ApiResponse<ClinicResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Clinic updated", clinicService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Deactivate a clinic (Admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        clinicService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Clinic deactivated", null));
    }
}
