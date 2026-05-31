package com.docqueue.doctor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "doctor_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "day_of_week"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_slots", nullable = false)
    @Builder.Default
    private int maxSlots = 20;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}
