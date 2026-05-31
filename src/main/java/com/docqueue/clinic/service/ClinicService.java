package com.docqueue.clinic.service;

import com.docqueue.clinic.dto.ClinicRequest;
import com.docqueue.clinic.dto.ClinicResponse;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.clinic.repository.ClinicRepository;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ClinicResponse> listAll(Pageable pageable) {
        return new PagedResponse<>(
                clinicRepository.findByIsActiveTrue(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClinicResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ClinicResponse create(ClinicRequest request) {
        Clinic clinic = Clinic.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .city(request.getCity())
                .state(request.getState())
                .isActive(true)
                .build();
        clinic = clinicRepository.save(clinic);
        log.info("Clinic created: id={}, name={}", clinic.getId(), clinic.getName());
        return toResponse(clinic);
    }

    @Transactional
    public ClinicResponse update(Long id, ClinicRequest request) {
        Clinic clinic = findById(id);
        clinic.setName(request.getName());
        clinic.setAddress(request.getAddress());
        clinic.setPhone(request.getPhone());
        clinic.setEmail(request.getEmail());
        clinic.setCity(request.getCity());
        clinic.setState(request.getState());
        return toResponse(clinicRepository.save(clinic));
    }

    @Transactional
    public void delete(Long id) {
        Clinic clinic = findById(id);
        clinic.setActive(false);
        clinic.setDeletedAt(LocalDateTime.now());
        clinicRepository.save(clinic);
        log.info("Clinic soft-deleted: id={}", id);
    }

    private Clinic findById(Long id) {
        return clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", id));
    }

    private ClinicResponse toResponse(Clinic c) {
        return ClinicResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .address(c.getAddress())
                .phone(c.getPhone())
                .email(c.getEmail())
                .city(c.getCity())
                .state(c.getState())
                .isActive(c.isActive())
                .build();
    }
}
