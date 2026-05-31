package com.docqueue.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Clinic analytics DTO for the admin dashboard.
 * Contains daily summary, peak hour analysis, and doctor performance.
 */
@Getter
@Builder
public class ClinicAnalyticsDto {

    // Daily summary
    private Long   clinicId;
    private String clinicName;
    private int    totalAppointmentsToday;
    private int    completedToday;
    private int    cancelledToday;
    private int    currentlyWaiting;
    private double avgWaitTimeMinutes;

    // Peak hours: hour (0–23) → appointment count
    private Map<Integer, Long> peakHourDistribution;

    // Doctor performance: doctorName → throughput (completed today)
    private Map<String, Integer> doctorThroughput;

    // Weekly trend: date string → count
    private Map<String, Long> weeklyTrend;
}
