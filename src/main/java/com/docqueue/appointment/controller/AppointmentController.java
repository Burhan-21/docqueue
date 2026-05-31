package com.docqueue.appointment.controller;

import com.docqueue.appointment.dto.*;
import com.docqueue.appointment.entity.Appointment.CancelledBy;
import com.docqueue.appointment.service.AppointmentService;
import com.docqueue.common.response.ApiResponse;
import com.docqueue.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment booking, cancellation, and management")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @Valid @RequestBody BookAppointmentRequest request,
            Authentication auth) {
        AppointmentResponse response = appointmentService.book(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked successfully", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my appointments (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> getMyAppointments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        PagedResponse<AppointmentResponse> response = appointmentService.getMyAppointments(
                auth, PageRequest.of(page, size, Sort.by("scheduledAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam CancelledBy cancelledBy,
            Authentication auth) {
        AppointmentResponse response = appointmentService.cancel(id, reason, cancelledBy, auth);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled", response));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request,
            Authentication auth) {
        AppointmentResponse response = appointmentService.reschedule(id, request, auth);
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled", response));
    }

    @GetMapping("/doctor/today")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get doctor's appointments for today")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getDoctorToday(Authentication auth) {
        List<AppointmentResponse> response = appointmentService.getDoctorTodayAppointments(auth);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
