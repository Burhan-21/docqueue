package com.docqueue.appointment.service;

import com.docqueue.appointment.dto.*;
import com.docqueue.appointment.entity.Appointment;
import com.docqueue.appointment.entity.Appointment.AppointmentStatus;
import com.docqueue.appointment.entity.Appointment.CancelledBy;
import com.docqueue.appointment.repository.AppointmentRepository;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.common.exception.BusinessException;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.doctor.entity.Doctor;
import com.docqueue.doctor.repository.DoctorRepository;
import com.docqueue.notification.service.NotificationService;
import com.docqueue.patient.entity.Patient;
import com.docqueue.patient.repository.PatientRepository;
import com.docqueue.queue.service.QueueService;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private QueueService queueService;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patientUser;
    private Patient patient;
    private User doctorUser;
    private Doctor doctor;
    private Clinic clinic;

    @BeforeEach
    public void setUp() {
        patientUser = User.builder().id(1L).email("patient@example.com").name("Patient Test").build();
        patient = Patient.builder().id(2L).user(patientUser).build();

        clinic = Clinic.builder().id(3L).name("Clinic Test").build();
        doctorUser = User.builder().id(4L).name("Doctor Test").build();
        doctor = Doctor.builder()
                .id(5L)
                .user(doctorUser)
                .clinic(clinic)
                .specialization("Cardiology")
                .avgConsultMin(15)
                .build();
    }

    @Test
    public void book_Success() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorId(5L);
        request.setScheduledAt(LocalDateTime.now().plusHours(2));
        request.setNotes("Regular checkup");

        when(authentication.getName()).thenReturn("patient@example.com");
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(5L)).thenReturn(Optional.of(doctor));

        // Max quota checks
        when(appointmentRepository.countByPatientIdAndStatusInAndDeletedAtIsNull(eq(2L), anyList())).thenReturn(0L);
        // Booking conflict & availability checks (matches both countBookedSlots calls)
        when(appointmentRepository.countBookedSlots(eq(5L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);
        // Max token check
        when(appointmentRepository.findMaxTokenForDay(eq(5L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);

        AppointmentResponse response = appointmentService.book(request, authentication);

        assertNotNull(response);
        assertEquals("Doctor Test", response.getDoctorName());
        assertEquals("Cardiology", response.getDoctorSpecialization());
        assertEquals("Clinic Test", response.getClinicName());
        assertEquals(1, response.getTokenNumber());
        assertEquals("CONFIRMED", response.getStatus());

        verify(appointmentRepository).save(any(Appointment.class));
        verify(queueService).enqueue(any(Appointment.class));
        verify(notificationService).sendBookingConfirmation(any(Appointment.class));
    }

    @Test
    public void book_TooEarly_ThrowsException() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorId(5L);
        request.setScheduledAt(LocalDateTime.now().plusMinutes(10)); // < 30 minutes in future

        when(authentication.getName()).thenReturn("patient@example.com");
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(5L)).thenReturn(Optional.of(doctor));

        assertThrows(BusinessException.class, () -> appointmentService.book(request, authentication));
    }

    @Test
    public void book_QuotaExceeded_ThrowsException() {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorId(5L);
        request.setScheduledAt(LocalDateTime.now().plusHours(2));

        when(authentication.getName()).thenReturn("patient@example.com");
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(5L)).thenReturn(Optional.of(doctor));

        // Active appointments quota met/exceeded
        when(appointmentRepository.countByPatientIdAndStatusInAndDeletedAtIsNull(eq(2L), anyList())).thenReturn(5L);

        assertThrows(BusinessException.class, () -> appointmentService.book(request, authentication));
    }

    @Test
    public void cancel_Success() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .doctor(doctor)
                .patient(patient)
                .clinic(clinic)
                .status(AppointmentStatus.CONFIRMED)
                .tokenNumber(5)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.cancel(10L, "Out of town", CancelledBy.PATIENT, authentication);

        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(queueService).cancelQueueEntry(10L);
        verify(notificationService).sendCancellationNotification(appointment);
    }

    @Test
    public void cancel_CompletedAppointment_ThrowsException() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThrows(BusinessException.class, () -> appointmentService.cancel(10L, "Change plan", CancelledBy.PATIENT, authentication));
    }

    @Test
    public void reschedule_Success() {
        Appointment appointment = Appointment.builder()
                .id(10L)
                .doctor(doctor)
                .patient(patient)
                .clinic(clinic)
                .status(AppointmentStatus.CONFIRMED)
                .tokenNumber(5)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();

        RescheduleRequest request = new RescheduleRequest();
        request.setNewScheduledAt(LocalDateTime.now().plusHours(3));
        request.setReason("Change of time");

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.countBookedSlots(eq(5L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);
        when(appointmentRepository.findMaxTokenForDay(eq(5L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2);

        AppointmentResponse response = appointmentService.reschedule(10L, request, authentication);

        assertNotNull(response);
        assertEquals(3, response.getTokenNumber());
        assertEquals("CONFIRMED", response.getStatus());

        verify(queueService).cancelQueueEntry(10L);
        verify(appointmentRepository).save(appointment);
        verify(queueService).enqueue(appointment);
        verify(notificationService).sendRescheduleNotification(appointment);
    }
}
