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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authManager;
    @Mock private CaptchaService captchaService;
    @Mock private EmailService emailService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(authService, "accessExpiryMs", 900000L);
    }

    @Test
    public void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");
        request.setPassword("Password123");

        Role patientRole = new Role(1L, "PATIENT");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.register(request);

        assertTrue(response.isOtpRequired());
        assertEquals("Registration successful. OTP sent to your email.", response.getMessage());

        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
        verify(emailService).send(eq("john@example.com"), anyString(), anyString());
        verify(valueOperations).set(eq("otp:john@example.com"), anyString(), any(Duration.class));
    }

    @Test
    public void register_EmailExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@example.com");

        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("Password123");

        User user = User.builder()
                .email("login@example.com")
                .isActive(true)
                .build();

        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(request);

        assertTrue(response.isOtpRequired());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(valueOperations).set(eq("otp:login@example.com"), anyString(), any(Duration.class));
    }

    @Test
    public void resendOtp_Success() {
        OtpSendRequest request = new OtpSendRequest();
        request.setEmail("resend@example.com");
        request.setCaptchaToken("token123");

        User user = User.builder().email("resend@example.com").build();

        when(captchaService.verifyToken("token123")).thenReturn(true);
        when(userRepository.findByEmail("resend@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.resendOtp(request);

        assertTrue(response.isOtpRequired());
        assertEquals("A new OTP has been sent to your email.", response.getMessage());
        verify(emailService).send(eq("resend@example.com"), anyString(), anyString());
    }

    @Test
    public void resendOtp_CaptchaFailed_ThrowsException() {
        OtpSendRequest request = new OtpSendRequest();
        request.setEmail("resend@example.com");
        request.setCaptchaToken("invalid");

        when(captchaService.verifyToken("invalid")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.resendOtp(request));
    }

    @Test
    public void verifyOtp_Success() {
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setEmail("verify@example.com");
        request.setCode("123456");

        User user = User.builder()
                .id(10L)
                .name("Verify User")
                .email("verify@example.com")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:attempts:verify@example.com")).thenReturn(null);
        when(valueOperations.get("otp:verify@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("verify@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh_token");

        AuthResponse response = authService.verifyOtp(request);

        assertFalse(response.isOtpRequired());
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals(10L, response.getUserId());

        verify(redisTemplate).delete("otp:verify@example.com");
        verify(redisTemplate).delete("otp:attempts:verify@example.com");
    }

    @Test
    public void verifyOtp_TooManyAttempts_ThrowsException() {
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setEmail("blocked@example.com");
        request.setCode("123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:attempts:blocked@example.com")).thenReturn(5);

        assertThrows(BusinessException.class, () -> authService.verifyOtp(request));
    }

    @Test
    public void verifyOtp_InvalidOtp_IncrementsAttempts() {
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setEmail("wrong@example.com");
        request.setCode("000000");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:attempts:wrong@example.com")).thenReturn(2);
        when(valueOperations.get("otp:wrong@example.com")).thenReturn("123456");

        assertThrows(BusinessException.class, () -> authService.verifyOtp(request));
        verify(valueOperations).increment("otp:attempts:wrong@example.com");
    }
}
