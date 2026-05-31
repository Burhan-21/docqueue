package com.docqueue.doctor.entity;

import com.docqueue.clinic.entity.Clinic;
import com.docqueue.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(length = 100)
    private String specialization;

    @Column(length = 200)
    private String qualification;

    @Column(name = "avg_consult_min", nullable = false)
    @Builder.Default
    private int avgConsultMin = 15;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
