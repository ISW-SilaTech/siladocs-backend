package com.siladocs.application.dto;

import java.time.LocalDate;

// Using Records (Java 16+)

public record CourseRequest(
        Long curriculumId, // ID of the curriculum it belongs to
        Long careerId,     // ID of the career (needed for creation/update)
        String code,
        String name,
        String faculty,    // May come from frontend or derived in backend
        Integer syllabusCount, // Usually managed internally, frontend might not send
        Integer year,
        String status,
        LocalDate publicationDate // Can be null
) {}