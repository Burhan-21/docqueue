package com.docqueue.notification.service;

import com.docqueue.appointment.entity.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Notification orchestrator.
 * All methods are @Async — never blocks request thread.
 * Delegates to SmsService and EmailService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final SmsService   smsService;

    @Async
    public void sendBookingConfirmation(Appointment appointment) {
        String email = appointment.getPatient().getUser().getEmail();
        String phone = appointment.getPatient().getUser().getPhone();
        String name  = appointment.getPatient().getUser().getName();

        String subject = "Appointment Confirmed — Token #" + appointment.getTokenNumber();
        String body    = buildConfirmationEmail(appointment);

        emailService.send(email, subject, body);
        if (phone != null) {
            smsService.send(phone, "Hi " + name + ", your appointment is confirmed. Token #"
                    + appointment.getTokenNumber() + " with Dr. "
                    + appointment.getDoctor().getUser().getName()
                    + " on " + appointment.getScheduledAt().toLocalDate() + ".");
        }
    }

    @Async
    public void sendCancellationNotification(Appointment appointment) {
        String email = appointment.getPatient().getUser().getEmail();
        String phone = appointment.getPatient().getUser().getPhone();

        emailService.send(email, "Appointment Cancelled",
                "Your appointment #" + appointment.getTokenNumber() + " has been cancelled.");

        if (phone != null) {
            smsService.send(phone, "Your appointment #" + appointment.getTokenNumber()
                    + " has been cancelled. Reason: " + appointment.getCancelReason());
        }
    }

    @Async
    public void sendRescheduleNotification(Appointment appointment) {
        String email = appointment.getPatient().getUser().getEmail();
        String phone = appointment.getPatient().getUser().getPhone();

        emailService.send(email, "Appointment Rescheduled",
                "Your appointment has been rescheduled to " + appointment.getScheduledAt()
                        + ". New token: #" + appointment.getTokenNumber());

        if (phone != null) {
            smsService.send(phone, "Appointment rescheduled to "
                    + appointment.getScheduledAt().toLocalDate()
                    + ". Token #" + appointment.getTokenNumber());
        }
    }

    @Async
    public void sendQueueDelayNotification(String phone, String name, int delayMinutes) {
        if (phone != null) {
            smsService.send(phone, "Hi " + name + ", there's a " + delayMinutes
                    + " min delay at the clinic today. We apologize for the inconvenience.");
        }
    }

    private String buildConfirmationEmail(Appointment appointment) {
        return """
            Dear %s,
            
            Your appointment has been confirmed.
            
            Doctor: Dr. %s (%s)
            Clinic: %s
            Date & Time: %s
            Token Number: #%d
            
            Please arrive 10 minutes before your scheduled time.
            You will receive an SMS reminder 30 minutes before your appointment.
            
            Thank you for choosing our platform.
            """.formatted(
                appointment.getPatient().getUser().getName(),
                appointment.getDoctor().getUser().getName(),
                appointment.getDoctor().getSpecialization(),
                appointment.getClinic().getName(),
                appointment.getScheduledAt(),
                appointment.getTokenNumber()
        );
    }
}
