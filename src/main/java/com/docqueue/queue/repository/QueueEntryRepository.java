package com.docqueue.queue.repository;

import com.docqueue.queue.entity.QueueEntry;
import com.docqueue.queue.entity.QueueEntry.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    // All waiting entries for a doctor, ordered by position
    List<QueueEntry> findByDoctorIdAndStatusOrderByQueuePositionAsc(
            Long doctorId, QueueStatus status);

    // Specific appointment's queue entry
    Optional<QueueEntry> findByAppointmentId(Long appointmentId);

    // Count waiting ahead of this position
    int countByDoctorIdAndStatusAndQueuePositionLessThan(
            Long doctorId, QueueStatus status, int position);

    // Get the current IN_PROGRESS entry
    Optional<QueueEntry> findByDoctorIdAndStatus(Long doctorId, QueueStatus status);

    // Bulk reorder queue positions after skip/cancel
    @Modifying
    @Query("""
        UPDATE QueueEntry q
        SET q.queuePosition = q.queuePosition - 1
        WHERE q.doctor.id = :doctorId
          AND q.status = 'WAITING'
          AND q.queuePosition > :position
    """)
    void decrementPositionsAfter(@Param("doctorId") Long doctorId,
                                 @Param("position") int position);

    // Full queue snapshot for broadcast
    @Query("""
        SELECT q FROM QueueEntry q
        JOIN FETCH q.appointment a
        JOIN FETCH a.patient p
        JOIN FETCH p.user u
        WHERE q.doctor.id = :doctorId
          AND q.status IN ('WAITING', 'IN_PROGRESS')
        ORDER BY q.queuePosition ASC
    """)
    List<QueueEntry> findActiveQueueByDoctor(@Param("doctorId") Long doctorId);

    // Analytics: count waiting patients for an entire clinic
    long countByClinicIdAndStatus(Long clinicId, QueueStatus status);

    // Analytics: completed queue entries today for a clinic
    @Query("""
        SELECT q FROM QueueEntry q
        WHERE q.clinic.id = :clinicId
          AND q.status = 'COMPLETED'
          AND q.completedAt BETWEEN :start AND :end
    """)
    List<QueueEntry> findCompletedTodayByClinic(
            @Param("clinicId") Long clinicId,
            @Param("start")    java.time.LocalDateTime start,
            @Param("end")      java.time.LocalDateTime end);
}
