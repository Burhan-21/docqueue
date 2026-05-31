package com.docqueue.common.seed;

import com.docqueue.clinic.entity.Clinic;
import com.docqueue.clinic.repository.ClinicRepository;
import com.docqueue.doctor.entity.Doctor;
import com.docqueue.doctor.entity.DoctorAvailability;
import com.docqueue.doctor.entity.DoctorAvailability.DayOfWeek;
import com.docqueue.doctor.repository.DoctorAvailabilityRepository;
import com.docqueue.doctor.repository.DoctorRepository;
import com.docqueue.patient.entity.Patient;
import com.docqueue.patient.entity.Patient.Gender;
import com.docqueue.patient.repository.PatientRepository;
import com.docqueue.user.entity.Role;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.RoleRepository;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Only run seeding in local or dev profiles when database is unpopulated
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (!activeProfiles.contains("local") && !activeProfiles.contains("dev")) {
            log.debug("Seeding skipped: not in local/dev active profile");
            return;
        }

        if (clinicRepository.count() > 0) {
            log.debug("Seeding skipped: database already has clinic data");
            return;
        }

        log.info("Starting local/dev database seeding...");

        // Ensure roles exist
        Role patientRole = roleRepository.findByName("PATIENT")
                .orElseGet(() -> roleRepository.save(new Role(null, "PATIENT")));
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseGet(() -> roleRepository.save(new Role(null, "DOCTOR")));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));

        // 1. Seed Clinic
        Clinic clinic = Clinic.builder()
                .name("City Care Clinic")
                .address("123 Main Street, Indiranagar, Bangalore")
                .phone("9876543210")
                .email("info@citycare.in")
                .city("Bangalore")
                .state("Karnataka")
                .isActive(true)
                .build();
        clinic = clinicRepository.save(clinic);
        log.info("Seeded Clinic: {}", clinic.getName());

        // 2. Seed Doctors
        seedDoctor("Dr. Sarah Connor", "sarah@docqueue.in", "Cardiology", "MD, DM (Cardiology)", 
                "Specialist in interventional cardiology and cardiovascular health.", new BigDecimal("500.00"), clinic, doctorRole);

        seedDoctor("Dr. Bruce Banner", "bruce@docqueue.in", "General Medicine", "MBBS, MD (Internal Medicine)", 
                "Family physician specializing in general checkups and diagnostic medicine.", new BigDecimal("350.00"), clinic, doctorRole);

        // 3. Seed Patients
        seedPatient("John Doe", "john@docqueue.in", Gender.MALE, "O+", LocalDate.of(1990, 5, 12), patientRole);
        seedPatient("Jane Smith", "jane@docqueue.in", Gender.FEMALE, "A+", LocalDate.of(1995, 8, 24), patientRole);

        log.info("Local/dev database seeding completed successfully.");
    }

    private void seedDoctor(String name, String email, String spec, String qual, String bio, BigDecimal fee, Clinic clinic, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("Password@123"))
                .isActive(true)
                .build();
        user.getRoles().add(role);
        user = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(user)
                .clinic(clinic)
                .specialization(spec)
                .qualification(qual)
                .bio(bio)
                .consultationFee(fee)
                .avgConsultMin(15)
                .build();
        doctor = doctorRepository.save(doctor);

        // Seed availability for every day of the week
        for (DayOfWeek day : DayOfWeek.values()) {
            DoctorAvailability avail = DoctorAvailability.builder()
                    .doctor(doctor)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(17, 0))
                    .maxSlots(20)
                    .isActive(true)
                    .build();
            doctorAvailabilityRepository.save(avail);
        }

        log.info("Seeded Doctor: {} ({})", name, spec);
    }

    private void seedPatient(String name, String email, Gender gender, String bloodGroup, LocalDate dob, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("Password@123"))
                .isActive(true)
                .build();
        user.getRoles().add(role);
        user = userRepository.save(user);

        Patient patient = Patient.builder()
                .user(user)
                .gender(gender)
                .bloodGroup(bloodGroup)
                .dob(dob)
                .build();
        patientRepository.save(patient);

        log.info("Seeded Patient: {}", name);
    }
}
