package com.siladocs.domain.model;

// 🔹 Imports de JPA eliminados
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// ⬇️ Anotaciones JPA eliminadas
public class Document {

    private Long documentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String hash;
    private Instant uploadedAt = Instant.now();

    // Constructor usado en upload (sin id)
    public Document(String fileName, String fileType, Long fileSize, String hash, Instant uploadedAt) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.hash = hash;
        this.uploadedAt = uploadedAt;
    }
}