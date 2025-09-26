package com.siladocs.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents") // 👈 nombre de la tabla en Postgres
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId; // 👈 clave primaria (alineado con institutionId)

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType; // extensión o MIME type

    @Column(nullable = false)
    private Long fileSize; // tamaño en bytes

    @Column(nullable = false, unique = true)
    private String hash; // hash único para validar integridad

    @Column(name = "uploaded_at", updatable = false, nullable = false)
    private Instant uploadedAt = Instant.now();

    // Constructor usado en upload (sin id, porque JPA lo genera)
    public Document(String fileName, String fileType, Long fileSize, String hash, Instant uploadedAt) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.hash = hash;
        this.uploadedAt = uploadedAt;
    }
}
