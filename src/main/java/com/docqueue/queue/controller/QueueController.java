package com.docqueue.queue.controller;

import com.docqueue.common.response.ApiResponse;
import com.docqueue.queue.dto.QueueEntryDto;
import com.docqueue.queue.dto.QueueStateDto;
import com.docqueue.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "Real-time queue management and position queries")
@SecurityRequirement(name = "Bearer Authentication")
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current queue state for a doctor")
    public ResponseEntity<ApiResponse<List<QueueEntryDto>>> getQueue(
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(
                ApiResponse.success(queueService.getActiveQueue(doctorId)));
    }

    @GetMapping("/position/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get patient's current queue position")
    public ResponseEntity<ApiResponse<QueueEntryDto>> getPosition(
            @PathVariable Long appointmentId,
            Authentication auth) {
        return ResponseEntity.ok(
                ApiResponse.success(queueService.getQueueEntryForAppointment(appointmentId, auth)));
    }

    @PostMapping("/next/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Doctor calls next patient")
    public ResponseEntity<ApiResponse<Void>> callNext(
            @PathVariable Long doctorId,
            Authentication auth) {
        queueService.callNext(doctorId, auth);
        return ResponseEntity.ok(ApiResponse.success("Next patient called", null));
    }

    @PutMapping("/skip/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Skip a patient in the queue")
    public ResponseEntity<ApiResponse<Void>> skip(
            @PathVariable Long appointmentId,
            @RequestParam Long doctorId,
            Authentication auth) {
        queueService.skipPatient(appointmentId, doctorId, auth);
        return ResponseEntity.ok(ApiResponse.success("Patient skipped", null));
    }
}
