package com.docqueue.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * JavaMail email service.
 * Sends transactional emails (confirmation, reminders, cancellations).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
            // In production: save to notifications table with sent=false for retry
        }
    }
}
