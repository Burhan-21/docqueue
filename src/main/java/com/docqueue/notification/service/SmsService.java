package com.docqueue.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Twilio SMS service.
 * Sends transactional SMS notifications.
 * Gracefully no-ops if Twilio credentials are not configured (dev mode).
 */
@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private boolean twilioEnabled = false;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isBlank() &&
            authToken  != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            twilioEnabled = true;
            log.info("Twilio SMS service initialized");
        } else {
            log.warn("Twilio credentials not configured — SMS sending disabled");
        }
    }

    public void send(String to, String body) {
        if (!twilioEnabled) {
            log.info("[SMS MOCK] To: {} | Body: {}", to, body);
            return;
        }

        try {
            // Format Indian phone: +91XXXXXXXXXX
            String formattedTo = to.startsWith("+") ? to : "+91" + to;
            Message message = Message.creator(
                    new PhoneNumber(formattedTo),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("SMS sent to {} — SID: {}", to, message.getSid());
        } catch (Exception ex) {
            log.error("Failed to send SMS to {}: {}", to, ex.getMessage());
        }
    }
}
