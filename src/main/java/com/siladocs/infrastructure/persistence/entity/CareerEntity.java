package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate; // O Instant

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "careers")
public class CareerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "career_id")
    private Long id;

    @Column(nullable = false, unique = true) // Asumimos que el nombre es único
    private String name;

    @Column(nullable = false)
    private String faculty;

    @Column(nullable = false)
    private Integer cycles;

    @Column(name = "last_updated", nullable = false)
    private LocalDate lastUpdated; // O Instant

    @Column(nullable = false)
    private String status;

    // Podrías añadir aquí la relación OneToMany con MallaEntity si la defines
    // @OneToMany(mappedBy = "career", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<CurriculumEntity> curriculums = new ArrayList<>();
}