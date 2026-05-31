package com.docqueue.notification.scheduler;

import com.docqueue.appointment.entity.Appointment;
import com.docqueue.appointment.repository.AppointmentRepository;
import com.docqueue.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reminder scheduler.
 * Runs every minute, finds appointments within the next 30 minutes,
 * and sends SMS reminders.
 *
 * Uses DB flag (notifications table) to prevent duplicate sends.
 * In multi-instance deployments, add a distributed lock (ShedLock).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService   notificationService;

    @Scheduled(fixedDelay = 60_000) // every 60 seconds
    public void sendUpcomingReminders() {
        LocalDateTime from = LocalDateTime.now().plusMinutes(29);
        LocalDateTime to   = LocalDateTime.now().plusMinutes(31);

        List<Appointment> upcoming = appointmentRepository.findUpcomingForReminder(from, to);

        if (!upcoming.isEmpty()) {
            log.info("Sending {} appointment reminders", upcoming.size());
        }

        for (Appointment appointment : upcoming) {
            try {
                String phone = appointment.getPatient().getUser().getPhone();
                String name  = appointment.getPatient().getUser().getName();

                if (phone != null) {
                    notificationService.sendQueueDelayNotification(
                            phone, name, 0); // 0 = reminder, not delay
                }
            } catch (Exception ex) {
                log.error("Failed to send reminder for appointment {}: {}",
                        appointment.getId(), ex.getMessage());
            }
        }
    }
}
