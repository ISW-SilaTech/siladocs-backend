package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents") // nombre de la tabla en Postgres
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id; // clave primaria

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, unique = true)
    private String hash; // hash único (sin longitud fija para que sea flexible)

    @Column(nullable = false)
    private String fileType; // extensión o MIME type

    @Column(nullable = false)
    private Long fileSize; // tamaño en bytes

    @Column(name = "uploaded_at", updatable = false, nullable = false)
    private Instant uploadedAt = Instant.now();

    // Constructor sin id para persistencia (igual que InstitutionEntity)
    public DocumentEntity(String fileName, String hash, String fileType, Long fileSize) {
        this.fileName = fileName;
        this.hash = hash;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = Instant.now();
    }
}
