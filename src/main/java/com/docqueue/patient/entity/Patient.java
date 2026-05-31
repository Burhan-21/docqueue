package com.docqueue.patient.entity;

import com.docqueue.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Gender { MALE, FEMALE, OTHER }
}
