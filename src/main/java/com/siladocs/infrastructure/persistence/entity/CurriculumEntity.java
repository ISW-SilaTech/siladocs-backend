package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "curriculums") // Table name 'curriculums'
public class CurriculumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curriculum_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "course_count", nullable = false)
    private Integer courseCount;

    @Column(name = "total_credits", nullable = false)
    private Integer totalCredits;

    @Column(nullable = false)
    private String status;

    @Column(length = 1024) // Allow longer descriptions
    private String description;

    // --- Relationship ---
    @ManyToOne(fetch = FetchType.LAZY) // Many Curriculums to One Career
    @JoinColumn(name = "career_id", nullable = false) // Foreign key column
    private CareerEntity career; // Reference to the Career entity

    // You might add OneToMany relationship to CourseEntity later
    // @OneToMany(mappedBy = "curriculum", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<CourseEntity> courses = new ArrayList<>();
}