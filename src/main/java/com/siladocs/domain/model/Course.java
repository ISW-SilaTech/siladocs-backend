package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class Course {
    private Long id;
    private Long curriculumId; // Belongs to a Curriculum
    private Long careerId;     // Belongs to a Career (redundant if accessed via Curriculum, but useful for filtering)
    private String code;       // e.g., MAT101
    private String name;       // e.g., Programación I
    private String faculty;    // e.g., Ingeniería (could be derived from Career)
    private Integer syllabusCount; // N° Sílabos
    private Integer year;      // Año
    private String status;     // e.g., "Active", "Closed"
    private LocalDate publicationDate; // Publicación

    // Constructor for creating new courses
    public Course(Long curriculumId, Long careerId, String code, String name, String faculty, Integer syllabusCount, Integer year, String status, LocalDate publicationDate) {
        this.curriculumId = curriculumId;
        this.careerId = careerId;
        this.code = code;
        this.name = name;
        this.faculty = faculty;
        this.syllabusCount = syllabusCount != null ? syllabusCount : 0; // Default to 0
        this.year = year;
        this.status = status;
        this.publicationDate = publicationDate != null ? publicationDate : LocalDate.now(); // Default publication date
    }
}