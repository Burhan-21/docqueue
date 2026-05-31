package com.docqueue.analytics.service;

import com.docqueue.analytics.dto.ClinicAnalyticsDto;
import com.docqueue.appointment.entity.Appointment;
import com.docqueue.appointment.repository.AppointmentRepository;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.clinic.repository.ClinicRepository;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.queue.entity.QueueEntry;
import com.docqueue.queue.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analytics service aggregating clinic-level metrics.
 * All queries are read-only. Future optimization: CQRS read model or materialized views.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository  queueEntryRepository;
    private final ClinicRepository      clinicRepository;

    /**
     * Daily clinic summary — completions, cancellations, wait times.
     */
    @Transactional(readOnly = true)
    public ClinicAnalyticsDto getClinicSummary(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", clinicId));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);

        List<Appointment> todayAppts = appointmentRepository
                .findTodayAppointmentsForClinic(clinicId, startOfDay, endOfDay);

        long completed  = todayAppts.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED).count();
        long cancelled  = todayAppts.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.CANCELLED).count();

        // Currently waiting across all doctors in clinic
        long waiting = queueEntryRepository
                .countByClinicIdAndStatus(clinicId, QueueEntry.QueueStatus.WAITING);

        // Avg wait time for completed queue entries today
        double avgWait = queueEntryRepository
                .findCompletedTodayByClinic(clinicId, startOfDay, endOfDay)
                .stream()
                .filter(q -> q.getEstimatedWait() != null)
                .mapToInt(QueueEntry::getEstimatedWait)
                .average()
                .orElse(0);

        // Peak hour distribution (by scheduledAt hour)
        Map<Integer, Long> peakHours = todayAppts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getScheduledAt().getHour(),
                        Collectors.counting()));

        // Doctor throughput (completed today per doctor)
        Map<String, Integer> doctorThroughput = todayAppts.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        a -> a.getDoctor().getUser().getName(),
                        Collectors.summingInt(a -> 1)));

        // Weekly trend (last 7 days)
        Map<String, Long> weeklyTrend = buildWeeklyTrend(clinicId);

        return ClinicAnalyticsDto.builder()
                .clinicId(clinicId)
                .clinicName(clinic.getName())
                .totalAppointmentsToday(todayAppts.size())
                .completedToday((int) completed)
                .cancelledToday((int) cancelled)
                .currentlyWaiting((int) waiting)
                .avgWaitTimeMinutes(avgWait)
                .peakHourDistribution(peakHours)
                .doctorThroughput(doctorThroughput)
                .weeklyTrend(weeklyTrend)
                .build();
    }

    private Map<String, Long> buildWeeklyTrend(Long clinicId) {
        // Last 7 days appointment counts
        Map<String, Long> trend = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date  = LocalDate.now().minusDays(i);
            LocalDateTime s = date.atStartOfDay();
            LocalDateTime e = date.atTime(LocalTime.MAX);
            long count = appointmentRepository
                    .findTodayAppointmentsForClinic(clinicId, s, e).size();
            trend.put(date.toString(), count);
        }
        return trend;
    }
}
