-- V1__initial_schema.sql
-- Doctor Queue SaaS - Initial Database Schema
-- PostgreSQL Migration

-- =========================================
-- CLINICS
-- =========================================
CREATE TABLE clinics (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    address     TEXT,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    city        VARCHAR(100),
    state       VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at  TIMESTAMP    NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinic_active ON clinics(is_active);
CREATE INDEX idx_clinic_city ON clinics(city);

-- =========================================
-- ROLES
-- =========================================
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('PATIENT'), ('DOCTOR'), ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- =========================================
-- USERS
-- =========================================
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    phone      VARCHAR(20),
    password   VARCHAR(255)  NOT NULL,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP     NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_phone ON users(phone);
CREATE INDEX idx_user_active ON users(is_active);

-- =========================================
-- USER_ROLES (Junction)
-- =========================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- =========================================
-- PATIENTS
-- =========================================
CREATE TABLE patients (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE,
    dob         DATE,
    gender      VARCHAR(10) CHECK (gender IN ('MALE','FEMALE','OTHER')),
    blood_group VARCHAR(10),
    deleted_at  TIMESTAMP   NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================
-- DOCTORS
-- =========================================
CREATE TABLE doctors (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL UNIQUE,
    clinic_id        BIGINT       NOT NULL,
    specialization   VARCHAR(100),
    qualification    VARCHAR(200),
    avg_consult_min  INT          NOT NULL DEFAULT 15,
    bio              TEXT,
    consultation_fee DECIMAL(10,2),
    deleted_at       TIMESTAMP    NULL,
    FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id)
);

CREATE INDEX idx_doctor_clinic ON doctors(clinic_id);
CREATE INDEX idx_doctor_spec ON doctors(specialization);
CREATE INDEX ft_doctor_spec ON doctors USING GIN (to_tsvector('english', specialization));

-- =========================================
-- DOCTOR AVAILABILITY
-- =========================================
CREATE TABLE doctor_availability (
    id          BIGSERIAL PRIMARY KEY,
    doctor_id   BIGINT NOT NULL,
    day_of_week VARCHAR(15) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time  TIME   NOT NULL,
    end_time    TIME   NOT NULL,
    max_slots   INT    NOT NULL DEFAULT 20,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT uq_doc_day UNIQUE (doctor_id, day_of_week)
);

CREATE INDEX idx_avail_doctor ON doctor_availability(doctor_id);

-- =========================================
-- APPOINTMENTS
-- =========================================
CREATE TABLE appointments (
    id             BIGSERIAL PRIMARY KEY,
    patient_id     BIGINT       NOT NULL,
    doctor_id      BIGINT       NOT NULL,
    clinic_id      BIGINT       NOT NULL,
    scheduled_at   TIMESTAMP    NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
    token_number   INT          NOT NULL,
    notes          TEXT,
    cancelled_by   VARCHAR(20)  NULL CHECK (cancelled_by IN ('PATIENT','DOCTOR','ADMIN')),
    cancel_reason  VARCHAR(500) NULL,
    deleted_at     TIMESTAMP    NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id)  REFERENCES doctors(id),
    FOREIGN KEY (clinic_id)  REFERENCES clinics(id),
    CONSTRAINT uq_token_doctor_date UNIQUE (doctor_id, scheduled_at, token_number)
);

CREATE INDEX idx_appt_doctor_date ON appointments(doctor_id, scheduled_at);
CREATE INDEX idx_appt_patient ON appointments(patient_id);
CREATE INDEX idx_appt_status ON appointments(status);
CREATE INDEX idx_appt_scheduled ON appointments(scheduled_at);

-- =========================================
-- QUEUE ENTRIES
-- =========================================
CREATE TABLE queue_entries (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    doctor_id      BIGINT NOT NULL,
    clinic_id      BIGINT NOT NULL,
    queue_position INT    NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING','IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED')),
    estimated_wait INT    NULL,
    entered_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at      TIMESTAMP NULL,
    completed_at   TIMESTAMP NULL,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (doctor_id)      REFERENCES doctors(id),
    FOREIGN KEY (clinic_id)      REFERENCES clinics(id)
);

CREATE INDEX idx_queue_doctor_status ON queue_entries(doctor_id, status);
CREATE INDEX idx_queue_position ON queue_entries(doctor_id, queue_position);
CREATE INDEX idx_queue_entered ON queue_entries(entered_at);

-- =========================================
-- NOTIFICATIONS
-- =========================================
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    type       VARCHAR(10) NOT NULL CHECK (type IN ('SMS','EMAIL')),
    event      VARCHAR(30) NOT NULL CHECK (event IN ('BOOKING_CONFIRMED','REMINDER_30MIN','QUEUE_UPDATE','CANCELLED','RESCHEDULED','QUEUE_DELAY')),
    payload    JSONB,
    sent       BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at    TIMESTAMP   NULL,
    error_msg  TEXT        NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_sent ON notifications(sent);
CREATE INDEX idx_notif_event ON notifications(event);

-- =========================================
-- AUDIT LOGS
-- =========================================
CREATE TABLE audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NULL,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(100) NOT NULL,
    entity_id  BIGINT       NULL,
    old_value  JSONB        NULL,
    new_value  JSONB        NULL,
    ip_address VARCHAR(50)  NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity, entity_id);
CREATE INDEX idx_audit_date ON audit_logs(created_at);
