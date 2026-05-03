package com.siladocs.application.service;

import com.siladocs.application.dto.BulkCourseRequestDto;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkUploadService {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);

    private final CareerJpaRepository careerRepository;
    private final CurriculumJpaRepository curriculumRepository;
    private final CourseJpaRepository courseRepository;
    private final UserRepository userRepository;

    public BulkUploadService(CareerJpaRepository careerRepository,
            CurriculumJpaRepository curriculumRepository,
            CourseJpaRepository courseRepository,
            UserRepository userRepository) {
        this.careerRepository = careerRepository;
        this.curriculumRepository = curriculumRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BulkUploadResult processBulkCourses(List<BulkCourseRequestDto> requests, String userEmail) {
        log.info("Iniciando procesamiento de carga masiva de {} cursos por usuario: {}", requests.size(), userEmail);

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            BulkCourseRequestDto req = requests.get(i);
            int rowNum = i + 2;

            try {
                // ── Validate presence of required nested objects ──────────────
                if (req.getCarrera() == null || req.getCarrera().getNombre() == null
                        || req.getCarrera().getNombre().isBlank()) {
                    throw new IllegalArgumentException("Carrera.nombre es requerido");
                }
                if (req.getMalla() == null || req.getMalla().getNombre() == null
                        || req.getMalla().getNombre().isBlank()) {
                    throw new IllegalArgumentException("Malla.nombre es requerido");
                }
                if (req.getCurso() == null || req.getCurso().getNombre() == null
                        || req.getCurso().getNombre().isBlank()) {
                    throw new IllegalArgumentException("Curso.nombre es requerido");
                }

                // ── Step 1: Find or Create Career ─────────────────────────────
                CareerEntity career = findOrCreateCareer(req.getCarrera(), rowNum);

                // ── Step 2: Find or Create Curriculum (Malla) ─────────────────
                CurriculumEntity curriculum = findOrCreateCurriculum(req.getMalla(), career, rowNum);

                // ── Step 3: Find or Create Course ─────────────────────────────
                boolean created = findOrCreateCourse(req.getCurso(), career, curriculum, rowNum);
                if (created) {
                    successCount++;
                }

            } catch (IllegalArgumentException e) {
                log.error("Error en Fila {}: {}", rowNum, e.getMessage());
                errors.add("Fila " + rowNum + ": " + e.getMessage());
            } catch (Exception e) {
                log.error("Error inesperado en Fila {}: {}", rowNum, e.getMessage(), e);
                errors.add("Fila " + rowNum + ": Error inesperado - " + e.getMessage());
            }
        }

        log.info("Procesamiento finalizado. Éxito: {}, Errores: {}", successCount, errors.size());
        return new BulkUploadResult(successCount, errors);
    }

    // ── Find or Create Career ─────────────────────────────────────────────────
    private CareerEntity findOrCreateCareer(BulkCourseRequestDto.CarreraData data, int rowNum) {
        String nombre = data.getNombre().trim();

        return careerRepository.findByNameIgnoreCase(nombre).orElseGet(() -> {
            log.info("Fila {}: Carrera '{}' no existe — creando.", rowNum, nombre);

            CareerEntity career = new CareerEntity();
            career.setName(nombre);
            career.setFaculty(data.getFacultad() != null && !data.getFacultad().isBlank()
                    ? data.getFacultad().trim()
                    : "Sin Facultad");
            career.setCycles(data.getCiclos() != null && data.getCiclos() > 0 ? data.getCiclos() : 10);
            career.setStatus(data.getEstado() != null && !data.getEstado().isBlank()
                    ? data.getEstado().trim()
                    : "Activo");
            career.setLastUpdated(LocalDate.now());

            CareerEntity saved = careerRepository.save(career);
            log.info("Fila {}: Carrera '{}' creada con ID {}.", rowNum, nombre, saved.getId());
            return saved;
        });
    }

    // ── Find or Create Curriculum ─────────────────────────────────────────────
    private CurriculumEntity findOrCreateCurriculum(BulkCourseRequestDto.MallaData data,
            CareerEntity career, int rowNum) {
        String nombre = data.getNombre().trim();

        return curriculumRepository
                .findByNameIgnoreCaseAndCareerId(nombre, career.getId())
                .orElseGet(() -> {
                    log.info("Fila {}: Malla '{}' no existe para carrera '{}' — creando.",
                            rowNum, nombre, career.getName());

                    CurriculumEntity curriculum = new CurriculumEntity();
                    curriculum.setName(nombre);
                    curriculum.setCareer(career);
                    curriculum.setYear(data.getAño() != null ? data.getAño() : LocalDate.now().getYear());
                    curriculum.setCourseCount(data.getNumCursos() != null ? data.getNumCursos() : 0);
                    curriculum.setTotalCredits(data.getCreditos() != null ? data.getCreditos() : 0);
                    curriculum.setDescription(data.getDescripcion() != null ? data.getDescripcion() : "");
                    curriculum.setStatus(data.getEstado() != null && !data.getEstado().isBlank()
                            ? data.getEstado().trim()
                            : "Activo");

                    CurriculumEntity saved = curriculumRepository.save(curriculum);
                    log.info("Fila {}: Malla '{}' creada con ID {}.", rowNum, nombre, saved.getId());
                    return saved;
                });
    }

    // ── Find or Create Course ─────────────────────────────────────────────────
    // Returns true if a new course was created, false if it already existed.
    private boolean findOrCreateCourse(BulkCourseRequestDto.CursoData data,
            CareerEntity career, CurriculumEntity curriculum, int rowNum) {

        String nombre = data.getNombre().trim();
        String codigo = data.getCodigo() != null ? data.getCodigo().trim().toUpperCase() : null;

        // 1. If a code is provided and already exists — skip
        if (codigo != null && !codigo.isBlank()) {
            if (courseRepository.findByCodeIgnoreCase(codigo).isPresent()) {
                log.warn("Fila {}: Curso con código '{}' ya existe — omitiendo.", rowNum, codigo);
                return false;
            }
        }

        // 2. If a course with the same name already exists in this curriculum — skip
        if (courseRepository.existsByNameIgnoreCaseAndCurriculumId(nombre, curriculum.getId())) {
            log.warn("Fila {}: Curso '{}' ya existe en la malla '{}' — omitiendo.",
                    rowNum, nombre, curriculum.getName());
            return false;
        }

        // 3. Create the course
        log.info("Fila {}: Curso '{}' no existe — creando.", rowNum, nombre);

        CourseEntity course = new CourseEntity();
        course.setName(nombre);
        course.setCode(resolveCode(codigo, career, nombre, data.getCiclo(), rowNum));
        course.setCurriculum(curriculum);
        course.setCareer(career);
        course.setFaculty(career.getFaculty());
        course.setYear(data.getAño() != null ? data.getAño()
                : (curriculum.getYear() != null ? curriculum.getYear() : LocalDate.now().getYear()));
        course.setStatus(data.getEstado() != null && !data.getEstado().isBlank()
                ? data.getEstado().trim()
                : "Activo");
        course.setSyllabusCount(0);
        course.setPublicationDate(parseDate(data.getFechaPublicacion()));

        CourseEntity saved = courseRepository.save(course);
        log.info("Fila {}: Curso '{}' creado con ID {} y código '{}'.",
                rowNum, nombre, saved.getId(), saved.getCode());
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveCode(String providedCode, CareerEntity career, String courseName,
            Integer ciclo, int rowNum) {
        if (providedCode != null && !providedCode.isBlank()) {
            return providedCode;
        }
        // Auto-generate a code if not provided
        String careerPrefix = career.getName().replaceAll("[^A-Za-z]", "");
        careerPrefix = careerPrefix.length() >= 3
                ? careerPrefix.substring(0, 3).toUpperCase()
                : careerPrefix.toUpperCase();

        String coursePrefix = courseName.replaceAll("[^A-Za-z]", "");
        coursePrefix = coursePrefix.length() >= 2
                ? coursePrefix.substring(0, 2).toUpperCase()
                : coursePrefix.toUpperCase();

        int cycle = ciclo != null ? ciclo : 1;
        String candidate = careerPrefix + cycle + "0" + coursePrefix;

        // Ensure uniqueness by appending suffix if necessary
        int suffix = 1;
        String code = candidate;
        while (courseRepository.existsByCode(code)) {
            code = candidate + suffix++;
        }
        log.info("Fila {}: Código auto-generado '{}'.", rowNum, code);
        return code;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    // ── Result record ─────────────────────────────────────────────────────────
    public record BulkUploadResult(int successCount, List<String> errors) {
    }
}
