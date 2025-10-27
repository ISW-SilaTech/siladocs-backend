package com.siladocs.domain.model;

import lombok.AllArgsConstructor; // <-- ¡AÑADIR ESTA LÍNEA!
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor // <-- ¡AÑADIR ESTA ANOTACIÓN!
public class Institution {

    private Long institutionId;
    private String name;
    private String domain;
    private String status;
    private Instant createdAt;

    // Constructor para crear una nueva (este ya lo tenías)
    public Institution(String name, String domain, String status) {
        this.name = name;
        this.domain = domain;
        this.status = status;
        this.createdAt = Instant.now();
    }
}