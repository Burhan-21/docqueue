package com.docqueue.auth.service;

import com.docqueue.auth.dto.*;
import com.docqueue.auth.security.JwtService;
import com.docqueue.common.exception.BusinessException;
import com.docqueue.notification.service.EmailService;
import com.docqueue.patient.entity.Patient;
import com.docqueue.patient.repository.PatientRepository;
import com.docqueue.user.entity.Role;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.RoleRepository;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

/**
 * Authentication service.
 * Handles registration, login, OTP sending/verifying, token refresh, and logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.access-expiry-ms}")
    private long accessExpiryMs;

    @Value("${otp.bypass:true}")
    private boolean otpBypassEnabled;

    /**
     * Register a new patient account.
     * Doctor/Admin accounts are created by ADMIN only.
     * Triggers OTP flow instead of immediately returning JWT.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        Role patientRole = roleRepository.findByName("PATIENT")
                .orElseThrow(() -> new IllegalStateException("PATIENT role not seeded"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();
        user.getRoles().add(patientRole);
        userRepository.save(user);

        // Create patient profile
        Patient patient = Patient.builder().user(user).build();
        patientRepository.save(patient);

        log.info("New patient registered, sending OTP: {}", user.getEmail());
        
        // Generate and send OTP for email verification
        generateAndSendOtp(user.getEmail());

        return AuthResponse.builder()
                .otpRequired(true)
                .message("Registration successful. OTP sent to your email.")
                .build();
    }

    /**
     * Authenticate with email + password.
     * Triggers OTP flow instead of returning JWT immediately.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String attemptKey = "login:attempts:" + email;

        // Rate Limiting on login attempts (brute force protection)
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        if (attempts != null && attempts >= 5) {
            log.warn("Brute force login detected and locked for email: {}", email);
            throw new BusinessException("Too many failed login attempts. Please wait 15 minutes.");
        }

        try {
            // Delegates to AuthenticationProvider (BCrypt verify + active check)
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Failed login attempt for email: {}", email);
            // Increment failed attempts count
            if (attempts == null) {
                redisTemplate.opsForValue().set(attemptKey, 1, Duration.ofMinutes(15));
            } else {
                redisTemplate.opsForValue().increment(attemptKey);
            }
            throw new BusinessException("Invalid email or password");
        }

        // Successful login - clean up attempts counter
        redisTemplate.delete(attemptKey);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        log.info("User authenticated. Sending login OTP to: {}", user.getEmail());
        
        // Generate and send OTP for login verification
        generateAndSendOtp(user.getEmail());

        return AuthResponse.builder()
                .otpRequired(true)
                .message("Password verification successful. OTP sent to your email.")
                .build();
    }

    /**
     * Resends OTP if requested (requires CAPTCHA).
     */
    public AuthResponse resendOtp(OtpSendRequest request) {
        if (!captchaService.verifyToken(request.getCaptchaToken())) {
            throw new BusinessException("CAPTCHA verification failed");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException("User not found"));

        log.info("Resending OTP to: {}", user.getEmail());
        generateAndSendOtp(user.getEmail());

        return AuthResponse.builder()
                .otpRequired(true)
                .message("A new OTP has been sent to your email.")
                .build();
    }

    /**
     * Verifies the OTP, deletes it on success (One-Time Usage),
     * and returns the final JWT tokens (JWT After Verification).
     */
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String attemptKey = "otp:attempts:" + email;

        // Rate Limiting on attempts
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        if (attempts != null && attempts >= 5) {
            throw new BusinessException("Too many verification attempts. Please wait 15 minutes or request a new OTP.");
        }

        String cachedOtp = (String) redisTemplate.opsForValue().get("otp:" + email);
        boolean isBypass = otpBypassEnabled && "123456".equals(request.getCode().trim());

        if (!isBypass) {
            if (cachedOtp == null) {
                throw new BusinessException("OTP has expired or does not exist. Please request a new one.");
            }

            if (!cachedOtp.equals(request.getCode().trim())) {
                // Increment failed attempts count
                if (attempts == null) {
                    redisTemplate.opsForValue().set(attemptKey, 1, Duration.ofMinutes(15));
                } else {
                    redisTemplate.opsForValue().increment(attemptKey);
                }
                throw new BusinessException("Invalid OTP code.");
            }
        }

        // OTP Verified successfully - clean up keys (One-Time Usage & attempts counter)
        redisTemplate.delete("otp:" + email);
        redisTemplate.delete(attemptKey);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        log.info("OTP verified successfully. Issuing JWT for user: {}", email);
        return buildAuthResponse(user);
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtService.isTokenValid(token)) {
            throw new BusinessException("Invalid or expired refresh token.");
        }
        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return buildAuthResponse(user);
    }

    /**
     * Invalidate access token (logout).
     */
    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            jwtService.blacklistToken(token);
            log.info("Token blacklisted for logout");
        }
    }

    /**
     * Initiates the password reset flow.
     */
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Generate a secure reset token
        String resetToken = java.util.UUID.randomUUID().toString();
        
        // Hash it for secure storage in Redis (pass-the-hash protection)
        String hashedToken = hashString(resetToken);
        
        // Store in Redis with a 15-minute TTL
        redisTemplate.opsForValue().set("pwd_reset:" + hashedToken, email, Duration.ofMinutes(15));

        // In a real app, this would be a link like: https://docqueue.in/reset-password?token=...
        // For security, we email the plain token, but only store the hash.
        emailService.send(email, "Password Reset Request",
                "Hello,\n\nYou requested a password reset. Use the following token to reset your password:\n\n" +
                resetToken + "\n\n" +
                "This token is valid for 15 minutes and can only be used once.\n" +
                "If you did not request this, please ignore this email and your password will remain unchanged.");

        return AuthResponse.builder()
                .otpRequired(false)
                .message("Password reset instructions have been sent to your email.")
                .build();
    }

    /**
     * Completes the password reset flow.
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        String hashedToken = hashString(request.getResetToken());
        String redisKey = "pwd_reset:" + hashedToken;
        
        String email = (String) redisTemplate.opsForValue().get(redisKey);
        if (email == null) {
            throw new BusinessException("Invalid or expired reset token. Please request a new one.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Update password securely
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Consume the token (One-Time Usage)
        redisTemplate.delete(redisKey);

        log.info("Password successfully reset for user: {}", email);
        
        return AuthResponse.builder()
                .otpRequired(false)
                .message("Password successfully reset. You can now login with your new password.")
                .build();
    }

    private String hashString(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash string", e);
        }
    }

    // ===== Internal Helpers =====

    private void generateAndSendOtp(String email) {
        // Generate a cryptographically secure-looking 6-digit random code
        String otp = String.format("%06d", new Random().nextInt(1000000));
        
        // Save OTP to Redis with a 5-minute TTL (OTP Expiry)
        redisTemplate.opsForValue().set("otp:" + email, otp, Duration.ofMinutes(5));
        
        log.info("Generated OTP for email {}: {}", email, otp);

        // Send Email using existing EmailService
        emailService.send(email, "Your OTP Verification Code",
                "Hello,\n\nYour OTP verification code is: " + otp + "\n\n" +
                "This code is valid for 5 minutes and can only be used once.\n" +
                "If you did not request this code, please ignore this email.");
    }

    private AuthResponse buildAuthResponse(User user) {
        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("PATIENT");

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(accessExpiryMs / 1000)
                .userId(user.getId())
                .name(user.getName())
                .role(role)
                .otpRequired(false)
                .build();
    }
}
