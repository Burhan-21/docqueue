package com.docqueue.queue.entity;

import com.docqueue.appointment.entity.Appointment;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.doctor.entity.Doctor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "queue_position", nullable = false)
    private int queuePosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "estimated_wait")
    private Integer estimatedWait;  // minutes

    @Column(name = "entered_at", nullable = false, updatable = false)
    private LocalDateTime enteredAt;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        enteredAt = LocalDateTime.now();
    }

    public enum QueueStatus {
        WAITING, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED
    }
}
