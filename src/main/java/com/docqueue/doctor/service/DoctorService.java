package com.docqueue.doctor.service;

import com.docqueue.clinic.entity.Clinic;
import com.docqueue.clinic.repository.ClinicRepository;
import com.docqueue.common.exception.BusinessException;
import com.docqueue.common.exception.ResourceNotFoundException;
import com.docqueue.common.response.PagedResponse;
import com.docqueue.doctor.dto.AvailabilitySlotDto;
import com.docqueue.doctor.dto.DoctorRequest;
import com.docqueue.doctor.dto.DoctorResponse;
import com.docqueue.doctor.entity.Doctor;
import com.docqueue.doctor.entity.DoctorAvailability;
import com.docqueue.doctor.repository.DoctorAvailabilityRepository;
import com.docqueue.doctor.repository.DoctorRepository;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Doctor management service.
 * Handles CRUD, availability management, and slot calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository             doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final ClinicRepository             clinicRepository;
    private final UserRepository               userRepository;

    // ===== Public: Search & Profile =====

    /**
     * Search doctors by specialization. Results cached per page.
     */
    @Cacheable(value = "doctorSearch",
               key = "(#spec != null ? #spec : 'all') + ':page:' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public PagedResponse<DoctorResponse> search(String spec, Pageable pageable) {
        if (spec != null && !spec.isBlank()) {
            return new PagedResponse<>(
                    doctorRepository
                            .findBySpecializationContainingIgnoreCaseAndDeletedAtIsNull(spec, pageable)
                            .map(this::toResponse));
        }
        return new PagedResponse<>(doctorRepository.findAllActive(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        return toResponse(findDoctorById(id));
    }

    /**
     * Get available time slots for a doctor on a given date.
     * Returns list of LocalTime slots not yet booked.
     */
    @Cacheable(value = "slots", key = "'doctor:' + #doctorId + ':date:' + #date")
    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = findDoctorById(doctorId);

        DoctorAvailability.DayOfWeek dow = DoctorAvailability.DayOfWeek
                .valueOf(date.getDayOfWeek().name());

        DoctorAvailability availability = availabilityRepository
                .findByDoctorIdAndDayOfWeek(doctorId, dow)
                .orElseThrow(() -> new BusinessException(
                        "Doctor is not available on " + date.getDayOfWeek().getDisplayName(
                                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)));

        if (!availability.isActive()) {
            throw new BusinessException("Doctor is not available on this day.");
        }

        // Generate slots at avgConsultMin intervals
        List<String> slots = new ArrayList<>();
        LocalTime current = availability.getStartTime();
        LocalTime end     = availability.getEndTime();
        int intervalMin   = doctor.getAvgConsultMin();

        while (!current.plusMinutes(intervalMin).isAfter(end)) {
            slots.add(current.toString());
            current = current.plusMinutes(intervalMin);
        }

        return slots;
    }

    // ===== Doctor: Manage Own Availability =====

    @Transactional
    public List<AvailabilitySlotDto> setAvailability(Long doctorId,
                                                      List<AvailabilitySlotDto> slots) {
        Doctor doctor = findDoctorById(doctorId);

        for (AvailabilitySlotDto dto : slots) {
            if (dto.getEndTime().isBefore(dto.getStartTime()) ||
                    dto.getEndTime().equals(dto.getStartTime())) {
                throw new BusinessException("End time must be after start time for " + dto.getDayOfWeek());
            }

            DoctorAvailability existing = availabilityRepository
                    .findByDoctorIdAndDayOfWeek(doctorId, dto.getDayOfWeek())
                    .orElse(DoctorAvailability.builder().doctor(doctor).build());

            existing.setDayOfWeek(dto.getDayOfWeek());
            existing.setStartTime(dto.getStartTime());
            existing.setEndTime(dto.getEndTime());
            existing.setMaxSlots(dto.getMaxSlots());
            existing.setActive(dto.isActive());
            availabilityRepository.save(existing);
        }

        log.info("Updated availability for doctor {}: {} days configured", doctorId, slots.size());
        return slots;
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotDto> getAvailability(Long doctorId) {
        return availabilityRepository.findByDoctorIdAndIsActiveTrue(doctorId)
                .stream()
                .map(a -> {
                    AvailabilitySlotDto dto = new AvailabilitySlotDto();
                    dto.setDayOfWeek(a.getDayOfWeek());
                    dto.setStartTime(a.getStartTime());
                    dto.setEndTime(a.getEndTime());
                    dto.setMaxSlots(a.getMaxSlots());
                    dto.setActive(a.isActive());
                    return dto;
                }).toList();
    }

    // ===== Admin: Doctor CRUD =====

    @Transactional
    @CacheEvict(value = "doctorSearch", allEntries = true)
    public DoctorResponse create(DoctorRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        Clinic clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));

        if (doctorRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new BusinessException("A doctor profile already exists for this user.");
        }

        Doctor doctor = Doctor.builder()
                .user(user)
                .clinic(clinic)
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .avgConsultMin(request.getAvgConsultMin())
                .bio(request.getBio())
                .consultationFee(request.getConsultationFee())
                .build();

        doctor = doctorRepository.save(doctor);
        log.info("Doctor created: id={}, specialization={}", doctor.getId(), doctor.getSpecialization());
        return toResponse(doctor);
    }

    @Transactional
    @CacheEvict(value = {"doctorSearch", "slots"}, allEntries = true)
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = findDoctorById(id);
        Clinic clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getClinicId()));

        doctor.setClinic(clinic);
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setAvgConsultMin(request.getAvgConsultMin());
        doctor.setBio(request.getBio());
        doctor.setConsultationFee(request.getConsultationFee());
        doctorRepository.save(doctor);

        return toResponse(doctor);
    }

    @Transactional
    @CacheEvict(value = "doctorSearch", allEntries = true)
    public void delete(Long id) {
        Doctor doctor = findDoctorById(id);
        doctor.setDeletedAt(java.time.LocalDateTime.now());
        doctorRepository.save(doctor);
        log.info("Doctor soft-deleted: id={}", id);
    }

    // ===== Private Helpers =====

    private Doctor findDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
    }

    private DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .name(d.getUser().getName())
                .email(d.getUser().getEmail())
                .specialization(d.getSpecialization())
                .qualification(d.getQualification())
                .avgConsultMin(d.getAvgConsultMin())
                .bio(d.getBio())
                .consultationFee(d.getConsultationFee())
                .clinicId(d.getClinic().getId())
                .clinicName(d.getClinic().getName())
                .clinicCity(d.getClinic().getCity())
                .build();
    }
}
