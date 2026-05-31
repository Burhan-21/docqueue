package com.docqueue.appointment.service;

import com.docqueue.appointment.dto.*;
import com.docqueue.appointment.entity.Appointment;
import com.docqueue.appointment.entity.Appointment.AppointmentStatus;
import com.docqueue.appointment.entity.Appointment.CancelledBy;
import com.docqueue.appointment.repository.AppointmentRepository;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.common.exception.BusinessException;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.common.exception.UnauthorizedException;
import com.docqueue.common.response.PagedResponse;
import com.docqueue.doctor.entity.Doctor;
import com.docqueue.doctor.repository.DoctorRepository;
import com.docqueue.notification.service.NotificationService;
import com.docqueue.patient.entity.Patient;
import com.docqueue.patient.repository.PatientRepository;
import com.docqueue.queue.service.QueueService;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository     patientRepository;
    private final DoctorRepository      doctorRepository;
    private final QueueService          queueService;
    private final NotificationService   notificationService;
    private final UserRepository        userRepository;

    /**
     * Book an appointment for the authenticated patient.
     * Validates: slot availability, doctor availability, no double-booking.
     */
    @Transactional
    public AppointmentResponse book(BookAppointmentRequest request, Authentication auth) {
        Patient patient = resolvePatient(auth.getName());
        Doctor  doctor  = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", request.getDoctorId()));

        LocalDateTime scheduled = request.getScheduledAt();
        validateScheduledTime(scheduled);
        validateAvailability(doctor, scheduled);
        validateNoDoubleBooking(patient, doctor, scheduled);

        // Validate user quota (max 5 active appointments)
        long activeCount = appointmentRepository.countByPatientIdAndStatusInAndDeletedAtIsNull(
                patient.getId(),
                java.util.List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );
        if (activeCount >= 5) {
            throw new BusinessException("You have reached your maximum active appointments quota (5). Please complete or cancel an existing appointment first.");
        }

        int tokenNumber = generateToken(doctor, scheduled);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .clinic(doctor.getClinic())
                .scheduledAt(scheduled)
                .status(AppointmentStatus.CONFIRMED)
                .tokenNumber(tokenNumber)
                .notes(request.getNotes())
                .build();
        appointmentRepository.save(appointment);

        // Add to queue
        queueService.enqueue(appointment);

        // Send confirmation notifications (async)
        notificationService.sendBookingConfirmation(appointment);

        log.info("Appointment booked: id={}, patient={}, doctor={}, token={}",
                appointment.getId(), patient.getId(), doctor.getId(), tokenNumber);

        return mapToResponse(appointment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AppointmentResponse> getMyAppointments(Authentication auth, Pageable pageable) {
        Patient patient = resolvePatient(auth.getName());
        return new PagedResponse<>(
                appointmentRepository.findByPatientIdAndDeletedAtIsNull(patient.getId(), pageable)
                        .map(this::mapToResponse));
    }

    @Transactional
    public AppointmentResponse cancel(Long id, String reason,
                                      CancelledBy cancelledBy, Authentication auth) {
        Appointment appointment = findById(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed appointment.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Appointment is already cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);

        // Remove from queue
        queueService.cancelQueueEntry(appointment.getId());

        // Notify
        notificationService.sendCancellationNotification(appointment);

        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, RescheduleRequest request, Authentication auth) {
        Appointment appointment = findById(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Cannot reschedule a " + appointment.getStatus() + " appointment.");
        }

        LocalDateTime newTime = request.getNewScheduledAt();
        validateScheduledTime(newTime);
        validateAvailability(appointment.getDoctor(), newTime);

        int newToken = generateToken(appointment.getDoctor(), newTime);

        // Remove from old queue position
        queueService.cancelQueueEntry(appointment.getId());

        appointment.setScheduledAt(newTime);
        appointment.setTokenNumber(newToken);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        // Re-enqueue at new time
        queueService.enqueue(appointment);
        notificationService.sendRescheduleNotification(appointment);

        return mapToResponse(appointment);
    }

    @Transactional(readOnly = true)
    public java.util.List<AppointmentResponse> getDoctorTodayAppointments(Authentication auth) {
        Doctor doctor = doctorRepository.findByUserId(resolveUserId(auth))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);

        return appointmentRepository.findTodayAppointments(doctor.getId(), startOfDay, endOfDay)
                .stream().map(this::mapToResponse).toList();
    }

    // ===== Helpers =====

    private void validateScheduledTime(LocalDateTime scheduled) {
        if (scheduled.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessException("Appointment must be at least 30 minutes in the future.");
        }
    }

    private void validateAvailability(Doctor doctor, LocalDateTime scheduled) {
        // TODO: check doctor_availability slots for the day
        // Day-of-week check + max slots validation
        LocalDate date = scheduled.toLocalDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(LocalTime.MAX);

        int booked = appointmentRepository.countBookedSlots(doctor.getId(), start, end);
        // Simplified; should also cross-check DoctorAvailability.maxSlots
        if (booked >= 30) {
            throw new BusinessException("No available slots for the selected date.");
        }
    }

    private void validateNoDoubleBooking(Patient patient, Doctor doctor, LocalDateTime scheduled) {
        LocalDateTime windowStart = scheduled.minusMinutes(15);
        LocalDateTime windowEnd   = scheduled.plusMinutes(15);
        int count = appointmentRepository.countBookedSlots(doctor.getId(), windowStart, windowEnd);
        if (count > 0) {
            throw new BusinessException("This time slot is not available.");
        }
    }

    private int generateToken(Doctor doctor, LocalDateTime scheduled) {
        LocalDateTime start = scheduled.toLocalDate().atStartOfDay();
        LocalDateTime end   = scheduled.toLocalDate().atTime(LocalTime.MAX);
        return appointmentRepository.findMaxTokenForDay(doctor.getId(), start, end) + 1;
    }

    private Patient resolvePatient(String email) {
        return patientRepository.findByUserId(resolveUserIdByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    private Long resolveUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Long resolveUserId(Authentication auth) {
        return resolveUserIdByEmail(auth.getName());
    }

    private Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .doctorName(a.getDoctor().getUser().getName())
                .doctorSpecialization(a.getDoctor().getSpecialization())
                .clinicName(a.getClinic().getName())
                .patientName(a.getPatient().getUser().getName())
                .scheduledAt(a.getScheduledAt())
                .status(a.getStatus().name())
                .tokenNumber(a.getTokenNumber())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
