package com.docqueue.auth.controller;

import com.docqueue.auth.dto.*;
import com.docqueue.auth.service.AuthService;
import com.docqueue.auth.security.JwtAuthFilter;
import com.docqueue.common.ratelimit.RateLimitFilter;
import com.docqueue.auth.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.context.annotation.Import;
import com.docqueue.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestConfig.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JwtAuthFilter jwtAuthFilter() {
            return new JwtAuthFilter(null, null) {
                @Override
                protected void doFilterInternal(
                        jakarta.servlet.http.HttpServletRequest r, 
                        jakarta.servlet.http.HttpServletResponse rs, 
                        jakarta.servlet.FilterChain c) throws jakarta.servlet.ServletException, java.io.IOException {
                    c.doFilter(r, rs);
                }
            };
        }

        @Bean
        public RateLimitFilter rateLimitFilter() {
            return new RateLimitFilter() {
                @Override
                protected void doFilterInternal(
                        jakarta.servlet.http.HttpServletRequest r, 
                        jakarta.servlet.http.HttpServletResponse rs, 
                        jakarta.servlet.FilterChain c) throws jakarta.servlet.ServletException, java.io.IOException {
                    c.doFilter(r, rs);
                }
            };
        }
    }

    @Test
    public void register_ShouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password@123");

        AuthResponse response = AuthResponse.builder()
                .otpRequired(true)
                .message("OTP sent")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.otpRequired").value(true));
    }

    @Test
    public void login_ShouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("Password@123");

        AuthResponse response = AuthResponse.builder()
                .otpRequired(true)
                .message("OTP sent to login")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otpRequired").value(true));
    }

    @Test
    public void verifyOtp_ShouldReturnOk() throws Exception {
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setEmail("login@example.com");
        request.setCode("123456");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token123")
                .role("PATIENT")
                .build();

        when(authService.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token123"));
    }
}
