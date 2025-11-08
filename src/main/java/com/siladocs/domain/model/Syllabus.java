package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Syllabus {

    private Long id;
    private Long courseId;
    private Integer currentVersion;
    private String status;
    private String fileUrl;
    private String currentHash;
    private String lastChainHash;
    private Instant createdAt;
    private Instant updatedAt;

    // (Puedes añadir un constructor más simple si es necesario)
}