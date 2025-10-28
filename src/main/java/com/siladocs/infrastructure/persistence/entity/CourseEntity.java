package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false, unique = true) // Course codes are often unique
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String faculty; // Could be derived, but storing simplifies queries

    @Column(name = "syllabus_count", nullable = false)
    private Integer syllabusCount = 0; // Default value

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String status;

    @Column(name = "publication_date") // Nullable if not always published immediately
    private LocalDate publicationDate;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false) // FK to Curriculums
    private CurriculumEntity curriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_id", nullable = false) // FK to Careers
    private CareerEntity career;

    // Consider adding relationships to Syllabuses later
}