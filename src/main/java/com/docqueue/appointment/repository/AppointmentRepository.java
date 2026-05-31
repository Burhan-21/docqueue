package com.docqueue.appointment.repository;

import com.docqueue.appointment.entity.Appointment;
import com.docqueue.appointment.entity.Appointment.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Patient's appointments
    Page<Appointment> findByPatientIdAndDeletedAtIsNull(Long patientId, Pageable pageable);

    // Count active appointments for patient quota validation
    long countByPatientIdAndStatusInAndDeletedAtIsNull(Long patientId, List<AppointmentStatus> statuses);

    // Doctor's appointments for today
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.scheduledAt BETWEEN :start AND :end
          AND a.status NOT IN ('CANCELLED')
          AND a.deletedAt IS NULL
        ORDER BY a.tokenNumber ASC
    """)
    List<Appointment> findTodayAppointments(
            @Param("doctorId") Long doctorId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end);

    // Count booked slots for a doctor on a day (conflict check)
    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.scheduledAt BETWEEN :start AND :end
          AND a.status NOT IN ('CANCELLED')
          AND a.deletedAt IS NULL
    """)
    int countBookedSlots(
            @Param("doctorId") Long doctorId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end);

    // Max token for a doctor on a day
    @Query("""
        SELECT COALESCE(MAX(a.tokenNumber), 0) FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.scheduledAt BETWEEN :start AND :end
          AND a.deletedAt IS NULL
    """)
    int findMaxTokenForDay(
            @Param("doctorId") Long doctorId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end);

    // Upcoming appointments for reminder scheduler
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.scheduledAt BETWEEN :from AND :to
          AND a.status = 'CONFIRMED'
          AND a.deletedAt IS NULL
    """)
    List<Appointment> findUpcomingForReminder(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    // Clinic-scoped appointments for analytics
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.clinic.id = :clinicId
          AND a.scheduledAt BETWEEN :start AND :end
          AND a.deletedAt IS NULL
    """)
    List<Appointment> findTodayAppointmentsForClinic(
            @Param("clinicId") Long clinicId,
            @Param("start")    LocalDateTime start,
            @Param("end")      LocalDateTime end);
}
