package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "institutions")
public class InstitutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "institution_id")
    private Long institutionId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Devuelve el ID de la institución.
     * La anotación @Getter ya crea "getInstitutionId()",
     * pero este método "getId()" es útil como alias.
     */
    public Long getId() {
        return this.institutionId;
    }
}