package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // coincide con la columna de la tabla
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false) // nombre correcto en la tabla
    private String passwordHash;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN, ROLE_VIEWER, ROLE_EDITOR

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor sin id para persistencia
    public UserEntity(String name, String email, String passwordHash, String role, Long institutionId) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.institutionId = institutionId;
    }
}
