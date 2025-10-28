package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate; // O Instant si prefieres timestamp

@Getter
@Setter
@AllArgsConstructor
public class Career {
    private Long id;
    private String name;
    private String faculty;
    private Integer cycles; // Número de ciclos
    private LocalDate lastUpdated; // Fecha de actualización
    private String status; // Ej: "Activo", "En Revisión", "Inactivo", "Suspendido"

    // Constructor para crear nuevas carreras (sin ID)
    public Career(String name, String faculty, Integer cycles, String status) {
        this.name = name;
        this.faculty = faculty;
        this.cycles = cycles;
        this.status = status;
        this.lastUpdated = LocalDate.now(); // O Instant.now()
    }
}