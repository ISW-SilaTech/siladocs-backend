package com.siladocs.application.dto;

import java.time.LocalDate;

public record CourseResponse(
        Long id,
        Long curriculumId,
        String curriculumName, // Optional: for display
        Long careerId,
        String careerName,     // Optional: for display
        String code,
        String name,
        String faculty,
        Integer syllabusCount,
        Integer year,
        String status,
        String mallaStatus, // Showing Malla status from UI
        LocalDate publicationDate
) {}