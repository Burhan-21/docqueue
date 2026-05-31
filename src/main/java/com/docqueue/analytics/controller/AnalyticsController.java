package com.docqueue.analytics.controller;

import com.docqueue.analytics.dto.ClinicAnalyticsDto;
import com.docqueue.analytics.service.AnalyticsService;
import com.docqueue.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Analytics", description = "Clinic analytics and performance metrics (Admin only)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/clinic/{clinicId}/summary")
    @Operation(summary = "Get today's clinic summary — completions, cancellations, wait times")
    public ResponseEntity<ApiResponse<ClinicAnalyticsDto>> getClinicSummary(
            @PathVariable Long clinicId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getClinicSummary(clinicId)));
    }
}
