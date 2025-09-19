package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "institutions") // nombre de la tabla en Postgres
public class InstitutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "institution_id")
    private Long id; // clave primaria

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String domain; // dominio único

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, INACTIVE

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor sin id para persistencia
    public InstitutionEntity(String name, String domain, String status) {
        this.name = name;
        this.domain = domain;
        this.status = status;
    }
}
