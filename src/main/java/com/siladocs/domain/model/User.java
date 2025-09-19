package com.siladocs.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // nombre de la tabla en Postgres
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId; // Usar camelCase en Java

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash; // camelCase en Java

    @Column(nullable = false)
    private String role; // por ej: ROLE_ADMIN, ROLE_VIEWER, ROLE_EDITOR

    @Column(name = "institution_id", nullable = false)
    private Long institutionId; // camelCase

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor sin id para creación más limpia
    public User(String name, String email, String passwordHash, String role, Long institutionId) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.institutionId = institutionId;
    }
}
