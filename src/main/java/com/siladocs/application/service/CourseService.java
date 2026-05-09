package com.siladocs.application.service;

import com.siladocs.application.dto.CourseRequest;
import com.siladocs.application.dto.CourseResponse;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.CourseJpaRepository;
import com.siladocs.infrastructure.persistence.jparepository.SyllabusJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final SyllabusJpaRepository syllabusRepository;

    public CourseService(CourseJpaRepository courseRepository,
            CurriculumJpaRepository curriculumRepository,
            CareerJpaRepository careerRepository,
            SyllabusJpaRepository syllabusRepository) {
        this.courseRepository = courseRepository;
        this.curriculumRepository = curriculumRepository;
        this.careerRepository = careerRepository;
        this.syllabusRepository = syllabusRepository;
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
        log.info("Curso creado con ID: {}", savedEntity.getId());
        return entityToResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + id));
        return entityToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses(Long careerId, Long curriculumId) {
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
        log.info("Curso {} actualizado.", updatedEntity.getId());
        return entityToResponse(updatedEntity);
    }

    @Transactional
    public void deleteCourse(Long id) {
        log.warn("Eliminando curso con ID: {}", id);
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course not found with ID: " + id);
        }
        courseRepository.deleteById(id);
        log.info("Curso {} eliminado.", id);
    }

    private CourseResponse entityToResponse(CourseEntity entity) {
        Long curriculumId = (entity.getCurriculum() != null) ? entity.getCurriculum().getId() : null;
        String curriculumName = (entity.getCurriculum() != null) ? entity.getCurriculum().getName() : null;
        String mallaStatus = (entity.getCurriculum() != null) ? entity.getCurriculum().getStatus() : null;
        Long careerId = (entity.getCareer() != null) ? entity.getCareer().getId() : null;
        String careerName = (entity.getCareer() != null) ? entity.getCareer().getName() : null;

        // Contar silabus reales para este curso
        Integer realSyllabusCount = (int) syllabusRepository.countByCourse_Id(entity.getId());

        return new CourseResponse(
                entity.getId(),
                curriculumId,
                curriculumName,
                careerId,
                careerName,
                entity.getCode(),
                entity.getName(),
                entity.getFaculty(),
                realSyllabusCount,
                entity.getYear(),
                entity.getStatus(),
                mallaStatus,
                entity.getPublicationDate());
    }
}
