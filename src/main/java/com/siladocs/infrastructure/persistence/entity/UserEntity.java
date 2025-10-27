package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity // <-- CORRECTO
@Table(name = "users") // <-- CORRECTO
public class UserEntity {

    @Id // <-- CORRECTO
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <-- CORRECTO
    @Column(name = "user_id") // <-- CORRECTO: Mapea al nombre de columna físico
    private Long id; // <-- El nombre del campo Java puede ser 'id'

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor sin id (útil para JPA al crear nuevas entidades)
    public UserEntity(String name, String email, String passwordHash, String role, Long institutionId) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.institutionId = institutionId;
        this.createdAt = Instant.now(); // Asegura que se establezca
    }

    // El getter 'getId()' es generado por Lombok (@Getter)
    // El método getUserId() que tenías es redundante si el campo se llama 'id',
    // pero no causa el error. Puedes quitarlo si quieres.
    // public Long getUserId() { return this.id; }
}