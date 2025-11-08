package com.siladocs.application.service;

// 🔹 Imports de tu propio proyecto
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;
import com.siladocs.infrastructure.web.dto.BulkCourseRequestDto;

// 🔹 Imports de librerías (Spring, Hashing, Logging)
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 🔹 Imports de utilidades de Java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BulkUploadService {

    // 🔹 ----- DECLARACIÓN DE LOGGER (FALTABA) ----- 🔹
    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);

    private final CareerJpaRepository careerRepository;
    private final CurriculumJpaRepository curriculumRepository;
    private final CourseJpaRepository courseRepository;

    // 🔹 ----- NUEVAS DEPENDENCIAS ----- 🔹
    private final BlockchainService blockchainService;
    private final UserRepository userRepository;

    // 🔹 ----- CONSTRUCTOR ACTUALIZADO ----- 🔹
    public BulkUploadService(CareerJpaRepository careerRepository,
                             CurriculumJpaRepository curriculumRepository,
                             CourseJpaRepository courseRepository,
                             BlockchainService blockchainService,
                             UserRepository userRepository) {
        this.careerRepository = careerRepository;
        this.curriculumRepository = curriculumRepository;
        this.courseRepository = courseRepository;
        this.blockchainService = blockchainService;
        this.userRepository = userRepository;
    }

    /**
     * 🔹 MÉTODO ACTUALIZADO: Ahora acepta 'userEmail'
     */
    @Transactional
    public BulkUploadResult processBulkCourses(List<BulkCourseRequestDto> requests, String userEmail) {
        log.info("Iniciando procesamiento de carga masiva de {} cursos por usuario: {}", requests.size(), userEmail);
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // 🔹 ----- LÓGICA DE PRE-FETCH (COMPLETADA) ----- 🔹
        Map<String, CareerEntity> careersByName = careerRepository.findAll().stream()
                .collect(Collectors.toMap(CareerEntity::getName, c -> c, (c1, c2) -> c1)); // Maneja duplicados si existen
        Map<String, CurriculumEntity> curriculumsByName = curriculumRepository.findAll().stream()
                .collect(Collectors.toMap(CurriculumEntity::getName, m -> m, (m1, m2) -> m1));

        for (int i = 0; i < requests.size(); i++) {
            BulkCourseRequestDto req = requests.get(i);
            int rowNum = i + 2; // Asumiendo fila 1 es cabecera

            try {
                // 🔹 ----- LÓGICA DE VALIDACIÓN (COMPLETADA) ----- 🔹

                // 1. Encontrar Carrera
                CareerEntity career = careersByName.get(req.getCarrera());
                if (career == null) {
                    throw new IllegalArgumentException("Carrera no encontrada: '" + req.getCarrera() + "'");
                }

                // 2. Encontrar Malla
                CurriculumEntity curriculum = curriculumsByName.get(req.getMalla());
                if (curriculum == null) {
                    throw new IllegalArgumentException("Malla no encontrada: '" + req.getMalla() + "'");
                }

                // 3. Validar Consistencia
                if (!curriculum.getCareer().getId().equals(career.getId())) {
                    throw new IllegalArgumentException("La Malla '" + req.getMalla() + "' no pertenece a la Carrera '" + req.getCarrera() + "'");
                }

                // 4. Validar Ciclo
                int cycleNumber;
                try {
                    cycleNumber = Integer.parseInt(req.getCiclo());
                    if (cycleNumber < 1 || cycleNumber > career.getCycles()) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Ciclo inválido: '" + req.getCiclo() + "'. Debe ser un número entre 1 y " + career.getCycles());
                }

                // 5. Validar Duplicados (Curso)
                boolean exists = courseRepository.findByCurriculumId(curriculum.getId()).stream()
                        .anyMatch(course -> course.getName().equalsIgnoreCase(req.getCurso()));
                if (exists) {
                    log.warn("Fila {}: El curso '{}' ya existe en la malla '{}'. Omitiendo.", rowNum, req.getCurso(), req.getMalla());
                    continue; // Saltar al siguiente registro
                }

                // 6. 🔹 ----- CREAR ENTIDAD (COMPLETADO) ----- 🔹
                CourseEntity newCourse = new CourseEntity();
                newCourse.setName(req.getCurso());
                newCourse.setCurriculum(curriculum);
                newCourse.setCareer(career);
                newCourse.setFaculty(career.getFaculty()); // Hereda facultad de la carrera
                newCourse.setYear(curriculum.getYear());   // Hereda año de la malla
                newCourse.setStatus("Active"); // Default
                newCourse.setSyllabusCount(0); // Default
                newCourse.setPublicationDate(LocalDate.now()); // Default

                // 7. Generar Código
                String generatedCode = generateCourseCode(career.getName(), req.getCurso(), cycleNumber);
                if(courseRepository.existsByCode(generatedCode)){
                    log.warn("Fila {}: Código generado '{}' ya existe. Omitiendo.", rowNum, generatedCode);
                    continue;
                }
                newCourse.setCode(generatedCode);

                // 8. Guardar en SQL
                courseRepository.save(newCourse);
                successCount++;

                // 9. 🔹 Registrar en Blockchain 🔹
                try {
                    String dataHash = DigestUtils.sha256Hex(req.toString());
                    String txHash = blockchainService.registerSyllabusVersion(
                            newCourse.getId(),
                            dataHash,
                            userEmail, // ⬅️ Usa el email pasado como parámetro
                            "CURSO_CREADO (MASIVO)"
                    );
                    log.info("Fila {} (Curso ID {}): Registrado en Blockchain (Tx: {})", rowNum, newCourse.getId(), txHash);
                } catch (Exception e) {
                    log.error("Fila {}: Curso guardado en SQL (ID {}) pero ¡FALLÓ registro en Blockchain!: {}", rowNum, newCourse.getId(), e.getMessage());
                    // Lanzamos la excepción para revertir la creación del curso en SQL
                    throw new RuntimeException("Error en Blockchain para Fila " + rowNum + ", revirtiendo.", e);
                }

            } catch (IllegalArgumentException e) {
                // 🔹 ----- MANEJO DE ERROR (COMPLETADO) ----- 🔹
                log.error("Error en Fila {}: {}", rowNum, e.getMessage());
                errors.add("Fila " + rowNum + ": " + e.getMessage());
            } catch (Exception e) { // Captura errores inesperados
                log.error("Error inesperado en Fila {}: {}", rowNum, e.getMessage(), e);
                errors.add("Fila " + rowNum + ": Error inesperado - " + e.getMessage());
            }
        }

        log.info("Procesamiento de carga masiva finalizado. Éxito: {}, Errores: {}", successCount, errors.size());
        return new BulkUploadResult(successCount, errors);
    }

    // 🔹 ----- MÉTODO HELPER (AÑADIDO) ----- 🔹
    private String generateCourseCode(String careerName, String courseName, int cycle) {
        String careerPrefix = careerName.length() >= 3 ? careerName.substring(0, 3).toUpperCase() : careerName.toUpperCase();
        String coursePrefix = courseName.length() >= 3 ? courseName.substring(0, 3).toUpperCase() : courseName.toUpperCase();
        // Añade número aleatorio para reducir colisiones
        return careerPrefix + cycle + coursePrefix + (int)(Math.random() * 100);
    }

    // 🔹 ----- RECORD (YA ESTABA BIEN) ----- 🔹
    public record BulkUploadResult(int successCount, List<String> errors) {}
}