-- V1__initial_schema.sql
-- Doctor Queue SaaS - Initial Database Schema
-- MySQL 8.0+

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================
-- CLINICS
-- =========================================
CREATE TABLE IF NOT EXISTS clinics (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    address     TEXT,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    city        VARCHAR(100),
    state       VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at  DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_clinic_active (is_active),
    INDEX idx_clinic_city (city)
) ENGINE=InnoDB;

-- =========================================
-- ROLES
-- =========================================
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT IGNORE INTO roles (name) VALUES ('PATIENT'), ('DOCTOR'), ('ADMIN');

-- =========================================
-- USERS
-- =========================================
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    phone      VARCHAR(20),
    password   VARCHAR(255)  NOT NULL,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at DATETIME      NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_phone (phone),
    INDEX idx_user_active (is_active)
) ENGINE=InnoDB;

-- =========================================
-- USER_ROLES (Junction)
-- =========================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

-- =========================================
-- PATIENTS
-- =========================================
CREATE TABLE IF NOT EXISTS patients (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE,
    dob         DATE,
    gender      ENUM('MALE','FEMALE','OTHER'),
    blood_group VARCHAR(10),
    deleted_at  DATETIME    NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================
-- DOCTORS
-- =========================================
CREATE TABLE IF NOT EXISTS doctors (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL UNIQUE,
    clinic_id        BIGINT       NOT NULL,
    specialization   VARCHAR(100),
    qualification    VARCHAR(200),
    avg_consult_min  INT          NOT NULL DEFAULT 15,
    bio              TEXT,
    consultation_fee DECIMAL(10,2),
    deleted_at       DATETIME     NULL,
    FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id),
    INDEX idx_doctor_clinic (clinic_id),
    INDEX idx_doctor_spec   (specialization),
    FULLTEXT INDEX ft_doctor_spec (specialization)
) ENGINE=InnoDB;

-- =========================================
-- DOCTOR AVAILABILITY
-- =========================================
CREATE TABLE IF NOT EXISTS doctor_availability (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id   BIGINT NOT NULL,
    day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time  TIME   NOT NULL,
    end_time    TIME   NOT NULL,
    max_slots   INT    NOT NULL DEFAULT 20,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    UNIQUE KEY uq_doc_day (doctor_id, day_of_week),
    INDEX idx_avail_doctor (doctor_id)
) ENGINE=InnoDB;

-- =========================================
-- APPOINTMENTS
-- =========================================
CREATE TABLE IF NOT EXISTS appointments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id     BIGINT       NOT NULL,
    doctor_id      BIGINT       NOT NULL,
    clinic_id      BIGINT       NOT NULL,
    scheduled_at   DATETIME     NOT NULL,
    status         ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'PENDING',
    token_number   INT          NOT NULL,
    notes          TEXT,
    cancelled_by   ENUM('PATIENT','DOCTOR','ADMIN') NULL,
    cancel_reason  VARCHAR(500) NULL,
    deleted_at     DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id)  REFERENCES doctors(id),
    FOREIGN KEY (clinic_id)  REFERENCES clinics(id),
    INDEX idx_appt_doctor_date (doctor_id, scheduled_at),
    INDEX idx_appt_patient     (patient_id),
    INDEX idx_appt_status      (status),
    INDEX idx_appt_scheduled   (scheduled_at),
    UNIQUE KEY uq_token_doctor_date (doctor_id, scheduled_at, token_number)
) ENGINE=InnoDB;

-- =========================================
-- QUEUE ENTRIES
-- =========================================
CREATE TABLE IF NOT EXISTS queue_entries (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    doctor_id      BIGINT NOT NULL,
    clinic_id      BIGINT NOT NULL,
    queue_position INT    NOT NULL,
    status         ENUM('WAITING','IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED') NOT NULL DEFAULT 'WAITING',
    estimated_wait INT    NULL,
    entered_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at      DATETIME NULL,
    completed_at   DATETIME NULL,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (doctor_id)      REFERENCES doctors(id),
    FOREIGN KEY (clinic_id)      REFERENCES clinics(id),
    INDEX idx_queue_doctor_status (doctor_id, status),
    INDEX idx_queue_position      (doctor_id, queue_position),
    INDEX idx_queue_entered       (entered_at)
) ENGINE=InnoDB;

-- =========================================
-- NOTIFICATIONS
-- =========================================
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    type       ENUM('SMS','EMAIL')  NOT NULL,
    event      ENUM('BOOKING_CONFIRMED','REMINDER_30MIN','QUEUE_UPDATE','CANCELLED','RESCHEDULED','QUEUE_DELAY') NOT NULL,
    payload    JSON,
    sent       BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at    DATETIME    NULL,
    error_msg  TEXT        NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notif_user    (user_id),
    INDEX idx_notif_sent    (sent),
    INDEX idx_notif_event   (event)
) ENGINE=InnoDB;

-- =========================================
-- AUDIT LOGS
-- =========================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NULL,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(100) NOT NULL,
    entity_id  BIGINT       NULL,
    old_value  JSON         NULL,
    new_value  JSON         NULL,
    ip_address VARCHAR(50)  NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user   (user_id),
    INDEX idx_audit_entity (entity, entity_id),
    INDEX idx_audit_date   (created_at)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
