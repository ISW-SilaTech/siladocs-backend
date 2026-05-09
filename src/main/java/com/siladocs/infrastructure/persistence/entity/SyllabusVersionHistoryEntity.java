package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "syllabus_version_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SyllabusVersionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private SyllabusEntity syllabus;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "status", nullable = false)
    private String status; // DRAFT, SUBMITTED, APPROVED, ARCHIVED

    @Column(name = "fabric_tx_id")
    private String fabricTxId; // Para versiones que fueron registradas en blockchain

    @Column(name = "uploaded_by")
    private String uploadedBy; // Email del usuario que subió esta versión

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "notes", length = 500)
    private String notes; // Notas del cambio de versión

}
