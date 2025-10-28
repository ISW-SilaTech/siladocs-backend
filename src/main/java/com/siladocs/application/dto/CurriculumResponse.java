package com.siladocs.application.dto;

// Using Records (Java 16+)

// DTO for returning curriculum data (might include Career info)
public record CurriculumResponse(
        Long id,
        Long careerId,
        String careerName, // Optionally include career name for display
        String name,
        Integer year,
        Integer courseCount,
        Integer totalCredits,
        String status,
        String description
) {}