package com.siladocs.infrastructure.web.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Useful for logging

@Getter
@Setter
@ToString // Add this for easier debugging/logging
public class BulkCourseRequestDto {
    // Nombres deben coincidir con las CLAVES usadas en el frontend (y Excel headers idealmente)
    private String carrera; // Name of the Career
    private String malla;   // Name of the Curriculum (e.g., "Ingeniería de Software - Plan 2023")
    private String ciclo;   // Cycle number (as String initially)
    private String curso;   // Name of the Course to create

    // You might add optional fields later if needed, like code, faculty, etc.
}