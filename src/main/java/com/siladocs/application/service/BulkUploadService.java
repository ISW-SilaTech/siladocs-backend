package com.siladocs.application.service;

import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.web.dto.BulkCourseRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BulkUploadService {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);

    private final CareerJpaRepository careerRepository;
    private final CurriculumJpaRepository curriculumRepository;
    private final CourseJpaRepository courseRepository;

    public BulkUploadService(CareerJpaRepository careerRepository,
                             CurriculumJpaRepository curriculumRepository,
                             CourseJpaRepository courseRepository) {
        this.careerRepository = careerRepository;
        this.curriculumRepository = curriculumRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Processes a list of bulk course requests from an uploaded file.
     * Attempts to find existing careers and curriculums by name and create courses.
     * Returns a summary of created and failed records.
     */
    @Transactional // Process the whole batch in one transaction
    public BulkUploadResult processBulkCourses(List<BulkCourseRequestDto> requests) {
        log.info("Iniciando procesamiento de carga masiva de {} cursos.", requests.size());
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // --- Pre-fetch data for efficiency (optional but recommended for large files) ---
        // Fetch all careers and curriculums into maps for quick lookup
        Map<String, CareerEntity> careersByName = careerRepository.findAll().stream()
                .collect(Collectors.toMap(CareerEntity::getName, c -> c, (c1, c2) -> c1)); // Handle duplicates if needed
        Map<String, CurriculumEntity> curriculumsByName = curriculumRepository.findAll().stream()
                .collect(Collectors.toMap(CurriculumEntity::getName, m -> m, (m1, m2) -> m1));

        for (int i = 0; i < requests.size(); i++) {
            BulkCourseRequestDto req = requests.get(i);
            int rowNum = i + 2; // Assuming row 1 is header

            try {
                // 1. Find Career by Name
                CareerEntity career = careersByName.get(req.getCarrera());
                if (career == null) {
                    throw new IllegalArgumentException("Carrera no encontrada: '" + req.getCarrera() + "'");
                }

                // 2. Find Curriculum by Name
                CurriculumEntity curriculum = curriculumsByName.get(req.getMalla());
                if (curriculum == null) {
                    throw new IllegalArgumentException("Malla no encontrada: '" + req.getMalla() + "'");
                }

                // 3. Consistency Check: Ensure Curriculum belongs to Career
                if (!curriculum.getCareer().getId().equals(career.getId())) {
                    throw new IllegalArgumentException("La Malla '" + req.getMalla() + "' no pertenece a la Carrera '" + req.getCarrera() + "'");
                }

                // 4. Validate Ciclo (asumiendo que es parte del curso, no un modelo separado)
                int cycleNumber;
                try {
                    cycleNumber = Integer.parseInt(req.getCiclo());
                    if (cycleNumber < 1 || cycleNumber > career.getCycles()) { // Check against career max cycles
                        throw new NumberFormatException(); // Reuse catch block
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Ciclo inválido: '" + req.getCiclo() + "'. Debe ser un número entre 1 y " + career.getCycles());
                }

                // 5. Check if Course already exists (e.g., by Name or Code within Curriculum)
                // Assuming Course Name should be unique within a Curriculum for this bulk load
                boolean exists = courseRepository.findByCurriculumId(curriculum.getId()).stream()
                        .anyMatch(course -> course.getName().equalsIgnoreCase(req.getCurso()));
                if (exists) {
                    // Decide: Skip or Error? Let's skip with a warning for now.
                    log.warn("Fila {}: El curso '{}' ya existe en la malla '{}'. Omitiendo.", rowNum, req.getCurso(), req.getMalla());
                    // If you want it to be an error:
                    // throw new IllegalArgumentException("El curso '" + req.getCurso() + "' ya existe en la malla '" + req.getMalla() + "'");
                    continue; // Skip to next record
                }


                // 6. Create Course Entity (adapt fields as needed based on CourseEntity)
                CourseEntity newCourse = new CourseEntity();
                newCourse.setName(req.getCurso());
                newCourse.setCurriculum(curriculum);
                newCourse.setCareer(career);
                newCourse.setFaculty(career.getFaculty()); // Get faculty from career
                newCourse.setYear(curriculum.getYear());   // Get year from curriculum
                // Set default values or derive others if needed
                newCourse.setStatus("Active"); // Default status for bulk upload
                newCourse.setSyllabusCount(0); // Default syllabus count
                newCourse.setPublicationDate(LocalDate.now()); // Set publication date? Or null?

                // You'll need a unique 'code'. How is it generated? From name? Let's create a basic one.
                String generatedCode = generateCourseCode(career.getName(), req.getCurso(), cycleNumber);
                // Check if generated code is unique BEFORE setting and saving
                if(courseRepository.existsByCode(generatedCode)){
                    log.warn("Fila {}: Código generado '{}' ya existe. Omitiendo.", rowNum, generatedCode);
                    continue; // Skip
                }
                newCourse.setCode(generatedCode);


                courseRepository.save(newCourse);
                successCount++;

            } catch (IllegalArgumentException e) {
                log.error("Error en Fila {}: {}", rowNum, e.getMessage());
                errors.add("Fila " + rowNum + ": " + e.getMessage());
            } catch (Exception e) { // Catch unexpected errors
                log.error("Error inesperado en Fila {}: {}", rowNum, e.getMessage(), e);
                errors.add("Fila " + rowNum + ": Error inesperado - " + e.getMessage());
            }
        }

        log.info("Procesamiento de carga masiva finalizado. Éxito: {}, Errores: {}", successCount, errors.size());
        return new BulkUploadResult(successCount, errors);
    }

    // --- Helper Methods ---

    // Simple code generator (customize as needed)
    private String generateCourseCode(String careerName, String courseName, int cycle) {
        String careerPrefix = careerName.length() >= 3 ? careerName.substring(0, 3).toUpperCase() : careerName.toUpperCase();
        String coursePrefix = courseName.length() >= 3 ? courseName.substring(0, 3).toUpperCase() : courseName.toUpperCase();
        return careerPrefix + cycle + coursePrefix + (int)(Math.random() * 100); // Add random number for uniqueness
    }

    // Result class to return summary
    public record BulkUploadResult(int successCount, List<String> errors) {}

}