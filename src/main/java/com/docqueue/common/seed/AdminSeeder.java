package com.docqueue.common.seed;

import com.docqueue.user.entity.Role;
import com.docqueue.user.entity.User;
import com.docqueue.user.repository.RoleRepository;
import com.docqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a default ADMIN account on first startup.
 * Credentials are read from environment variables — no hardcoded secrets.
 *
 * Env vars:
 *  - ADMIN_EMAIL    (default: admin@docqueue.in)
 *  - ADMIN_PASSWORD (default: Admin@1234 — CHANGE IN PRODUCTION)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminEmail    = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@docqueue.in");
        String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin@1234");

        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Admin account already exists: {}", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not seeded by Flyway"));

        User admin = User.builder()
                .name("System Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .isActive(true)
                .build();
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("Default admin account created: {} (CHANGE PASSWORD IN PRODUCTION)", adminEmail);
    }
}
