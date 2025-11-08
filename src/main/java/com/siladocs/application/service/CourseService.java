package com.siladocs.application.service;

import com.siladocs.application.dto.CourseRequest;
import com.siladocs.application.dto.CourseResponse;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseJpaRepository courseRepository;
    private final CurriculumJpaRepository curriculumRepository;
    private final CareerJpaRepository careerRepository;

    private final BlockchainService blockchainService;
    private final UserRepository userRepository;

    public CourseService(CourseJpaRepository courseRepository,
                         CurriculumJpaRepository curriculumRepository,
                         CareerJpaRepository careerRepository,
                         BlockchainService blockchainService,
                         UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.curriculumRepository = curriculumRepository;
        this.careerRepository = careerRepository;
        this.blockchainService = blockchainService;
        this.userRepository = userRepository;
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        log.info("Creando curso con código '{}' para curriculum ID {}", request.code(), request.curriculumId());

        CurriculumEntity curriculum = curriculumRepository.findById(request.curriculumId())
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + request.curriculumId()));
        CareerEntity career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new EntityNotFoundException("Career not found with ID: " + request.careerId()));

        if (!curriculum.getCareer().getId().equals(career.getId())) {
            throw new IllegalArgumentException("Curriculum ID " + request.curriculumId() +
                    " does not belong to Career ID " + request.careerId());
        }

        if (courseRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Course code '" + request.code() + "' already exists.");
        }

        CourseEntity entity = new CourseEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setFaculty(request.faculty());
        entity.setSyllabusCount(request.syllabusCount() != null ? request.syllabusCount() : 0);
        entity.setYear(request.year());
        entity.setStatus(request.status());
        entity.setPublicationDate(request.publicationDate());
        entity.setCurriculum(curriculum);
        entity.setCareer(career);

        CourseEntity savedEntity = courseRepository.save(entity);
        log.info("Curso guardado en SQL con ID: {}", savedEntity.getId());

        try {
            String userEmail = getAuthenticatedUserEmail();
            String dataHash = DigestUtils.sha256Hex(request.toString());

            String txHash = blockchainService.registerSyllabusVersion(
                    savedEntity.getId(),
                    dataHash,
                    userEmail,
                    "CURSO_CREADO"
            );
            log.info("Curso {} registrado en Blockchain. TxHash: {}", savedEntity.getId(), txHash);

        } catch (Exception e) {
            log.error("¡FALLO CRÍTICO! No se pudo registrar en Blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar en Blockchain. El curso no fue creado.", e);
        }

        return entityToResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        log.debug("Fetching course with ID: {}", id);
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + id));
        return entityToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses(Long careerId, Long curriculumId) {
        log.debug("Fetching courses. Filter - careerId: {}, curriculumId: {}", careerId, curriculumId);
        List<CourseEntity> entities;
        if (curriculumId != null) {
            entities = courseRepository.findByCurriculumId(curriculumId);
        } else if (careerId != null) {
            entities = courseRepository.findByCareerId(careerId);
        } else {
            entities = courseRepository.findAll();
        }
        return entities.stream().map(this::entityToResponse).collect(Collectors.toList());
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        log.info("Actualizando curso con ID: {}", id);
        CourseEntity existingEntity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + id));

        CurriculumEntity curriculum = curriculumRepository.findById(request.curriculumId())
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + request.curriculumId()));
        CareerEntity career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new EntityNotFoundException("Career not found with ID: " + request.careerId()));

        if (!curriculum.getCareer().getId().equals(career.getId())) {
            throw new IllegalArgumentException("Curriculum ID " + request.curriculumId() +
                    " does not belong to Career ID " + request.careerId());
        }

        if (!existingEntity.getCode().equals(request.code()) && courseRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Course code '" + request.code() + "' already exists.");
        }

        existingEntity.setCode(request.code());
        existingEntity.setName(request.name());
        existingEntity.setFaculty(request.faculty());
        existingEntity.setSyllabusCount(request.syllabusCount() != null ? request.syllabusCount() : 0);
        existingEntity.setYear(request.year());
        existingEntity.setStatus(request.status());
        existingEntity.setPublicationDate(request.publicationDate());
        existingEntity.setCurriculum(curriculum);
        existingEntity.setCareer(career);

        CourseEntity updatedEntity = courseRepository.save(existingEntity);
        log.info("Curso {} actualizado en SQL.", updatedEntity.getId());

        try {
            String userEmail = getAuthenticatedUserEmail();
            String dataHash = DigestUtils.sha256Hex(request.toString());

            String txHash = blockchainService.registerSyllabusVersion(
                    updatedEntity.getId(),
                    dataHash,
                    userEmail,
                    "CURSO_ACTUALIZADO"
            );
            log.info("Actualización de Curso {} registrada en Blockchain. TxHash: {}", updatedEntity.getId(), txHash);

        } catch (Exception e) {
            log.error("¡FALLO CRÍTICO! No se pudo registrar la actualización en Blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar en Blockchain. La actualización no fue completada.", e);
        }

        return entityToResponse(updatedEntity);
    }

    @Transactional
    public void deleteCourse(Long id) {
        log.warn("Eliminando curso con ID: {}", id);
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + id));

        try {
            String userEmail = getAuthenticatedUserEmail();
            String dataHash = DigestUtils.sha256Hex(entity.toString());

            String txHash = blockchainService.registerSyllabusVersion(
                    id,
                    dataHash,
                    userEmail,
                    "CURSO_ELIMINADO"
            );
            log.info("Eliminación de Curso {} registrada en Blockchain. TxHash: {}", id, txHash);

        } catch (Exception e) {
            log.error("¡FALLO CRÍTICO! No se pudo registrar la eliminación en Blockchain: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar en Blockchain. La eliminación fue cancelada.", e);
        }

        courseRepository.deleteById(id);
        log.info("Curso {} eliminado de SQL.", id);
    }

    // ⬇️ 🔹 --- MÉTODO HELPER CORREGIDO --- 🔹 ⬇️
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Comprueba si la autenticación es nula, no está autenticada, o es el "usuario anónimo"
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            // Si tu SecurityConfig está en modo "permitAll", esto es normal.
            // Si está en modo "authenticated", esto significa que el filtro JWT falló.
            log.warn("No se encontró usuario autenticado. Usando 'system@siladocs.com' para el log de blockchain.");
            // Devolvemos un email genérico para que las pruebas (con permitAll) no fallen.
            // Cuando actives la seguridad, este bloque 'if' nunca debería ejecutarse
            // en un endpoint protegido, porque SecurityConfig lo bloqueará (401/403) antes.
            return "system@siladocs.com";
        }

        // Si está autenticado, devuelve el email del usuario (que es el 'name')
        return authentication.getName();
    }

    private CourseResponse entityToResponse(CourseEntity entity) {
        Long curriculumId = (entity.getCurriculum() != null) ? entity.getCurriculum().getId() : null;
        String curriculumName = (entity.getCurriculum() != null) ? entity.getCurriculum().getName() : null;
        String mallaStatus = (entity.getCurriculum() != null) ? entity.getCurriculum().getStatus() : null;
        Long careerId = (entity.getCareer() != null) ? entity.getCareer().getId() : null;
        String careerName = (entity.getCareer() != null) ? entity.getCareer().getName() : null;

        return new CourseResponse(
                entity.getId(),
                curriculumId,
                curriculumName,
                careerId,
                careerName,
                entity.getCode(),
                entity.getName(),
                entity.getFaculty(),
                entity.getSyllabusCount(),
                entity.getYear(),
                entity.getStatus(),
                mallaStatus,
                entity.getPublicationDate()
        );
    }
}
