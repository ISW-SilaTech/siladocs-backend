package com.siladocs.application.dto;

// Using Records (Java 16+)

// DTO for creating/updating a curriculum
public record CurriculumRequest(
        Long careerId, // ID of the career it belongs to
        String name,
        Integer year,
        Integer courseCount,
        Integer totalCredits,
        String status,
        String description
) {}