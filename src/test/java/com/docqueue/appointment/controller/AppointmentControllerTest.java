package com.docqueue.appointment.controller;

import com.docqueue.appointment.dto.*;
import com.docqueue.appointment.entity.Appointment.CancelledBy;
import com.docqueue.appointment.service.AppointmentService;
import com.docqueue.auth.security.JwtAuthFilter;
import com.docqueue.common.ratelimit.RateLimitFilter;
import com.docqueue.auth.security.UserDetailsServiceImpl;
import com.docqueue.common.response.ApiResponse;
import com.docqueue.common.response.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.context.annotation.Import;
import com.docqueue.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, AppointmentControllerTest.TestConfig.class})
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

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
    @WithMockUser(roles = "PATIENT")
    public void book_AsPatient_ShouldReturnCreated() throws Exception {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorId(1L);
        request.setScheduledAt(LocalDateTime.now().plusDays(1));
        request.setNotes("Slight fever");

        AppointmentResponse response = AppointmentResponse.builder()
                .id(10L)
                .tokenNumber(5)
                .doctorName("Dr. House")
                .build();

        when(appointmentService.book(any(BookAppointmentRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment booked successfully"))
                .andExpect(jsonPath("$.data.tokenNumber").value(5))
                .andExpect(jsonPath("$.data.doctorName").value("Dr. House"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    public void book_AsDoctor_ShouldReturnForbidden() throws Exception {
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorId(1L);
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/v1/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    public void getMyAppointments_AsPatient_ShouldReturnList() throws Exception {
        org.springframework.data.domain.Page<AppointmentResponse> mockPage = org.mockito.Mockito.mock(org.springframework.data.domain.Page.class);
        when(mockPage.getContent()).thenReturn(Collections.emptyList());
        when(mockPage.getNumber()).thenReturn(0);
        when(mockPage.getSize()).thenReturn(10);
        when(mockPage.getTotalElements()).thenReturn(0L);
        when(mockPage.getTotalPages()).thenReturn(0);
        when(mockPage.isLast()).thenReturn(true);

        PagedResponse<AppointmentResponse> pagedResponse = new PagedResponse<>(mockPage);

        when(appointmentService.getMyAppointments(any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/appointments/my")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    public void cancel_AsDoctor_ShouldReturnOk() throws Exception {
        AppointmentResponse response = AppointmentResponse.builder()
                .id(10L)
                .status("CANCELLED")
                .build();

        when(appointmentService.cancel(eq(10L), eq("No show"), eq(CancelledBy.DOCTOR), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/appointments/10/cancel")
                .with(csrf())
                .param("reason", "No show")
                .param("cancelledBy", "DOCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Appointment cancelled"));
    }
}
