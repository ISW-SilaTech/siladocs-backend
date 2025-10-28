package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Curriculum { // 'Malla' in Spanish
    private Long id;
    private Long careerId; // Foreign key reference to Career
    private String name; // e.g., "Ingeniería de Software - Plan 2023"
    private Integer year; // e.g., 2023
    private Integer courseCount;
    private Integer totalCredits;
    private String status; // e.g., "Activo", "Inactivo"
    private String description;

    // Constructor for creating new curriculums
    public Curriculum(Long careerId, String name, Integer year, Integer courseCount, Integer totalCredits, String status, String description) {
        this.careerId = careerId;
        this.name = name;
        this.year = year;
        this.courseCount = courseCount;
        this.totalCredits = totalCredits;
        this.status = status;
        this.description = description;
    }
}