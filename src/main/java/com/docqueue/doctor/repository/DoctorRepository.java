package com.docqueue.doctor.repository;

import com.docqueue.doctor.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Page<Doctor> findBySpecializationContainingIgnoreCaseAndDeletedAtIsNull(
            String specialization, Pageable pageable);

    Page<Doctor> findByClinicIdAndDeletedAtIsNull(Long clinicId, Pageable pageable);

    Optional<Doctor> findByUserId(Long userId);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL")
    Page<Doctor> findAllActive(Pageable pageable);
}
