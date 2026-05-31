package com.docqueue.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity for RBAC.
 * Pre-seeded via Flyway: PATIENT, DOCTOR, ADMIN.
 */
@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
