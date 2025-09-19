package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "syllabi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SyllabusEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long institutionId;
    private String courseCode;
    private String version;
    private String fileUrl;
    private String fileHash;
    private String status;
    private Instant createdAt;
}
