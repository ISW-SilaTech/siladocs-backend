package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "syllabi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SyllabusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relación con el Curso ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    // --- Campos de Versión y Trazabilidad ---

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "status", nullable = false)
    private String status; // Ej: "Borrador", "Aprobado"

    @Column(name = "file_url", nullable = false)
    private String fileUrl; // URL a Minio

    @Column(name = "current_file_hash", nullable = false, length = 64)
    private String currentHash; // Hash SHA-256 del archivo actual

    @Column(name = "last_chain_hash", nullable = false, length = 64)
    private String lastChainHash; // Hash del último bloque en la tabla de historial

    // --- Timestamps ---

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

}