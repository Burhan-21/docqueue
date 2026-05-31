package com.docqueue.patient.service;

import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.patient.dto.PatientProfileRequest;
import com.docqueue.patient.dto.PatientProfileResponse;
import com.docqueue.patient.entity.Patient;
import com.docqueue.patient.repository.PatientRepository;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository    userRepository;

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return toResponse(user, patient);
    }

    @Transactional
    public PatientProfileResponse updateProfile(String email, PatientProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patientRepository.save(patient);

        return toResponse(user, patient);
    }

    private PatientProfileResponse toResponse(User user, Patient patient) {
        return PatientProfileResponse.builder()
                .id(patient.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(patient.getDob())
                .gender(patient.getGender() != null ? patient.getGender().name() : null)
                .bloodGroup(patient.getBloodGroup())
                .build();
    }
}
