package com.docqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Doctor Appointment & Queue Management SaaS Platform
 * Production-grade modular monolith.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class DoctorQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorQueueApplication.class, args);
    }
}
