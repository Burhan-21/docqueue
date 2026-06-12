package com.docqueue.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to verify Google reCAPTCHA tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    @Value("${recaptcha.secret:6LeIxAcTAAAAAGG-vFI1TnFTxWfnC0CFU9onvZ17}")
    private String recaptchaSecret;

    private final RestTemplateBuilder restTemplateBuilder;

    /**
     * Verifies the reCAPTCHA response token against Google's API.
     * Always returns true for local development / testing keys.
     */
    public boolean verifyToken(String token) {
        // If no token, or secret is dummy/empty/blank, or token is dummy, allow bypass for local dev/testing
        if (token == null || token.isBlank() || 
            recaptchaSecret == null || recaptchaSecret.isBlank() ||
            "6LeIxAcTAAAAAGG-vFI1TnFTxWfnC0CFU9onvZ17".equals(recaptchaSecret) ||
            "g-recaptcha-response-dummy".equals(token.trim())) {
            log.info("Bypassing CAPTCHA verification (local dev, dummy secret, or dummy token configured)");
            return true;
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder.build();
            String url = "https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={response}";
            
            Map<String, String> params = new HashMap<>();
            params.put("secret", recaptchaSecret);
            params.put("response", token);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class, params);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("CAPTCHA verification succeeded");
                return true;
            }
            log.warn("CAPTCHA verification failed: {}", response);
            return false;
        } catch (Exception e) {
            log.error("Error occurred during CAPTCHA verification: {}", e.getMessage());
            return false;
        }
    }
}
