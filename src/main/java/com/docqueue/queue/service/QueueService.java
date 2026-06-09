package com.docqueue.queue.service;

import com.docqueue.appointment.entity.Appointment;
import com.docqueue.common.exception.BusinessException;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.queue.dto.QueueEntryDto;
import com.docqueue.queue.dto.QueueUpdateEvent;
import com.docqueue.queue.entity.QueueEntry;
import com.docqueue.queue.entity.QueueEntry.QueueStatus;
import com.docqueue.queue.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core queue management service.
 * Handles enqueue, call-next, skip, complete, and cancel operations.
 * Triggers WebSocket broadcast after every state change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueEntryRepository  queueEntryRepository;
    private final QueueBroadcastService broadcastService;
    private final com.docqueue.doctor.repository.DoctorRepository doctorRepository;
    private final com.docqueue.appointment.repository.AppointmentRepository appointmentRepository;

    /**
     * Add a newly booked appointment to the queue.
     */
    @Transactional
    @CacheEvict(value = "queue", key = "'doctor:' + #appointment.doctor.id")
    public QueueEntry enqueue(Appointment appointment) {
        List<QueueEntry> waitingEntries = queueEntryRepository
                .findByDoctorIdAndStatusOrderByQueuePositionAsc(
                        appointment.getDoctor().getId(), QueueStatus.WAITING);

        int nextPosition = waitingEntries.size() + 1;
        int estimatedWait = nextPosition * appointment.getDoctor().getAvgConsultMin();

        QueueEntry entry = QueueEntry.builder()
                .appointment(appointment)
                .doctor(appointment.getDoctor())
                .clinic(appointment.getClinic())
                .queuePosition(nextPosition)
                .status(QueueStatus.WAITING)
                .estimatedWait(estimatedWait)
                .build();

        queueEntryRepository.save(entry);
        log.info("Enqueued appointment {} at position {} for doctor {}",
                appointment.getId(), nextPosition, appointment.getDoctor().getId());

        broadcastQueueUpdate(appointment.getDoctor().getId());
        return entry;
    }

    /**
     * Doctor calls the next patient.
     * Marks current IN_PROGRESS as COMPLETED, next WAITING as IN_PROGRESS.
     */
    @Transactional
    @CacheEvict(value = "queue", key = "'doctor:' + #doctorId")
    public void callNext(Long doctorId, org.springframework.security.core.Authentication auth) {
        validateDoctorOwnership(doctorId, auth);
        // Complete any currently IN_PROGRESS
        queueEntryRepository.findByDoctorIdAndStatus(doctorId, QueueStatus.IN_PROGRESS)
                .ifPresent(current -> {
                    current.setStatus(QueueStatus.COMPLETED);
                    current.setCompletedAt(LocalDateTime.now());
                    queueEntryRepository.save(current);
                });

        // Promote next WAITING to IN_PROGRESS
        List<QueueEntry> waiting = queueEntryRepository
                .findByDoctorIdAndStatusOrderByQueuePositionAsc(doctorId, QueueStatus.WAITING);

        if (waiting.isEmpty()) {
            log.info("Queue empty for doctor {}", doctorId);
            broadcastQueueUpdate(doctorId);
            return;
        }

        QueueEntry next = waiting.get(0);
        next.setStatus(QueueStatus.IN_PROGRESS);
        next.setCalledAt(LocalDateTime.now());
        queueEntryRepository.save(next);

        recalculateWaitTimes(doctorId, next.getDoctor().getAvgConsultMin());

        log.info("Called next patient: appointmentId={}, position={}, doctorId={}",
                next.getAppointment().getId(), next.getQueuePosition(), doctorId);

        broadcastQueueUpdate(doctorId);
    }

    /**
     * Doctor skips a patient (e.g., patient not present).
     */
    @Transactional
    @CacheEvict(value = "queue", key = "'doctor:' + #doctorId")
    public void skipPatient(Long appointmentId, Long doctorId, org.springframework.security.core.Authentication auth) {
        validateDoctorOwnership(doctorId, auth);
        QueueEntry entry = queueEntryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));

        if (entry.getStatus() != QueueStatus.WAITING && entry.getStatus() != QueueStatus.IN_PROGRESS) {
            throw new BusinessException("Patient cannot be skipped in current status.");
        }

        int skippedPosition = entry.getQueuePosition();
        entry.setStatus(QueueStatus.SKIPPED);
        queueEntryRepository.save(entry);

        // Shift all positions after skipped patient down by 1
        queueEntryRepository.decrementPositionsAfter(doctorId, skippedPosition);
        recalculateWaitTimes(doctorId, entry.getDoctor().getAvgConsultMin());

        broadcastQueueUpdate(doctorId);
    }

    /**
     * Cancel a queue entry (on appointment cancellation).
     */
    @Transactional
    @CacheEvict(value = "queue", key = "'doctor:' + #appointmentId")
    public void cancelQueueEntry(Long appointmentId) {
        queueEntryRepository.findByAppointmentId(appointmentId).ifPresent(entry -> {
            int cancelledPosition = entry.getQueuePosition();
            Long doctorId         = entry.getDoctor().getId();

            entry.setStatus(QueueStatus.CANCELLED);
            queueEntryRepository.save(entry);

            queueEntryRepository.decrementPositionsAfter(doctorId, cancelledPosition);
            recalculateWaitTimes(doctorId, entry.getDoctor().getAvgConsultMin());

            broadcastQueueUpdate(doctorId);
        });
    }

    /**
     * Get the current queue snapshot for a doctor.
     */
    @Transactional(readOnly = true)
    public List<QueueEntryDto> getActiveQueue(Long doctorId) {
        return queueEntryRepository.findActiveQueueByDoctor(doctorId)
                .stream().map(this::toDto).toList();
    }

    /**
     * Get a patient's specific queue position.
     */
    @Transactional(readOnly = true)
    public QueueEntryDto getQueueEntryForAppointment(Long appointmentId, org.springframework.security.core.Authentication auth) {
        validatePatientOwnership(appointmentId, auth);
        return toDto(queueEntryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found")));
    }

    // ===== Internal =====

    private void validateDoctorOwnership(Long doctorId, org.springframework.security.core.Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
        com.docqueue.doctor.entity.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        if (!doctor.getUser().getEmail().equals(auth.getName())) {
            throw new com.docqueue.common.exception.UnauthorizedException("You do not have permission to modify this queue.");
        }
    }

    private void validatePatientOwnership(Long appointmentId, org.springframework.security.core.Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
        com.docqueue.appointment.entity.Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        if (!appointment.getPatient().getUser().getEmail().equals(auth.getName())) {
            throw new com.docqueue.common.exception.UnauthorizedException("You do not have permission to view this queue position.");
        }
    }

    /**
     * Recalculate estimated wait time for all WAITING entries.
     * Runs a single pass — no N+1.
     */
    private void recalculateWaitTimes(Long doctorId, int avgConsultMin) {
        List<QueueEntry> waiting = queueEntryRepository
                .findByDoctorIdAndStatusOrderByQueuePositionAsc(doctorId, QueueStatus.WAITING);

        for (int i = 0; i < waiting.size(); i++) {
            QueueEntry entry = waiting.get(i);
            entry.setEstimatedWait((i + 1) * avgConsultMin);
        }
        queueEntryRepository.saveAll(waiting);
    }

    private void broadcastQueueUpdate(Long doctorId) {
        List<QueueEntry> activeQueue = queueEntryRepository.findActiveQueueByDoctor(doctorId);
        QueueUpdateEvent event = QueueUpdateEvent.builder()
                .doctorId(doctorId)
                .patientsWaiting((int) activeQueue.stream()
                        .filter(e -> e.getStatus() == QueueStatus.WAITING).count())
                .currentToken(activeQueue.stream()
                        .filter(e -> e.getStatus() == QueueStatus.IN_PROGRESS)
                        .mapToInt(e -> e.getAppointment().getTokenNumber())
                        .findFirst().orElse(0))
                .estimatedWaitMinutes(activeQueue.stream()
                        .filter(e -> e.getStatus() == QueueStatus.WAITING)
                        .mapToInt(QueueEntry::getEstimatedWait)
                        .max().orElse(0))
                .updatedAt(LocalDateTime.now())
                .build();

        broadcastService.broadcastToDoctor(doctorId, event);
    }

    // ===== Mapping =====

    private QueueEntryDto toDto(QueueEntry q) {
        return QueueEntryDto.builder()
                .id(q.getId())
                .appointmentId(q.getAppointment().getId())
                .tokenNumber(q.getAppointment().getTokenNumber())
                .patientName(q.getAppointment().getPatient().getUser().getName())
                .queuePosition(q.getQueuePosition())
                .status(q.getStatus().name())
                .estimatedWait(q.getEstimatedWait())
                .scheduledAt(q.getAppointment().getScheduledAt())
                .enteredAt(q.getEnteredAt())
                .calledAt(q.getCalledAt())
                .completedAt(q.getCompletedAt())
                .doctorId(q.getDoctor().getId())
                .doctorName(q.getDoctor().getUser().getName())
                .build();
    }
}
