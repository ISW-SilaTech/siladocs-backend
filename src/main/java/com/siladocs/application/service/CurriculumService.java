package com.siladocs.application.service;

import com.siladocs.application.dto.CurriculumRequest;
import com.siladocs.application.dto.CurriculumResponse;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository; // Need this to fetch Career
import com.siladocs.infrastructure.persistence.jparepository.CurriculumJpaRepository;
// import com.siladocs.infrastructure.persistence.mapper.CurriculumMapper; // Mapper is less needed if converting DTOs directly
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CurriculumService {

    private static final Logger log = LoggerFactory.getLogger(CurriculumService.class);

    private final CurriculumJpaRepository curriculumRepository;
    private final CareerJpaRepository careerRepository; // Need access to careers
    // private final CurriculumMapper curriculumMapper; // Less needed here

    public CurriculumService(CurriculumJpaRepository curriculumRepository, CareerJpaRepository careerRepository) {
        this.curriculumRepository = curriculumRepository;
        this.careerRepository = careerRepository;
    }

    @Transactional
    public CurriculumResponse createCurriculum(CurriculumRequest request) {
        log.info("Creating curriculum '{}' for career ID {}", request.name(), request.careerId());

        // 1. Fetch the associated Career
        CareerEntity career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new EntityNotFoundException("Career not found with ID: " + request.careerId()));

        // 2. Validate uniqueness within the career
        if (curriculumRepository.existsByNameAndCareerId(request.name(), request.careerId())) {
            throw new IllegalArgumentException("Curriculum name '" + request.name() + "' already exists for this career.");
        }

        // 3. Create and save the entity
        CurriculumEntity entity = new CurriculumEntity();
        entity.setName(request.name());
        entity.setYear(request.year());
        entity.setCourseCount(request.courseCount());
        entity.setTotalCredits(request.totalCredits());
        entity.setStatus(request.status());
        entity.setDescription(request.description());
        entity.setCareer(career); // Set the relationship

        CurriculumEntity savedEntity = curriculumRepository.save(entity);
        log.info("Curriculum created with ID: {}", savedEntity.getId());
        return entityToResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculumById(Long id) {
        log.debug("Fetching curriculum with ID: {}", id);
        CurriculumEntity entity = curriculumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + id));
        return entityToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CurriculumResponse> getAllCurriculums(Long careerId) {
        log.debug("Fetching all curriculums for career ID: {}", careerId);
        List<CurriculumEntity> entities;
        if (careerId != null) {
            entities = curriculumRepository.findByCareerId(careerId);
        } else {
            entities = curriculumRepository.findAll(); // Or decide if this case is needed
        }
        return entities.stream().map(this::entityToResponse).collect(Collectors.toList());
    }


    @Transactional
    public CurriculumResponse updateCurriculum(Long id, CurriculumRequest request) {
        log.info("Updating curriculum with ID: {}", id);
        CurriculumEntity existingEntity = curriculumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + id));

        // Ensure the career exists (though unlikely to change, good practice)
        CareerEntity career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new EntityNotFoundException("Career not found with ID: " + request.careerId()));

        // Validate uniqueness if name changed
        if (!existingEntity.getName().equals(request.name()) &&
                curriculumRepository.existsByNameAndCareerId(request.name(), request.careerId())) {
            throw new IllegalArgumentException("Curriculum name '" + request.name() + "' already exists for this career.");
        }

        // Update fields
        existingEntity.setName(request.name());
        existingEntity.setYear(request.year());
        existingEntity.setCourseCount(request.courseCount());
        existingEntity.setTotalCredits(request.totalCredits());
        existingEntity.setStatus(request.status());
        existingEntity.setDescription(request.description());
        existingEntity.setCareer(career); // Update career if ID changed (less common)

        CurriculumEntity updatedEntity = curriculumRepository.save(existingEntity);
        log.info("Curriculum updated with ID: {}", updatedEntity.getId());
        return entityToResponse(updatedEntity);
    }

    @Transactional
    public void deleteCurriculum(Long id) {
        log.warn("Deleting curriculum with ID: {}", id);
        if (!curriculumRepository.existsById(id)) {
            throw new EntityNotFoundException("Curriculum not found with ID: " + id);
        }
        // Be careful: Deleting a curriculum might cascade delete courses if configured
        curriculumRepository.deleteById(id);
        log.info("Curriculum deleted with ID: {}", id);
    }


    // --- Helper to convert Entity to Response DTO ---
    private CurriculumResponse entityToResponse(CurriculumEntity entity) {
        // Handle potential null Career if relationship is optional (though it's not here)
        Long careerId = (entity.getCareer() != null) ? entity.getCareer().getId() : null;
        String careerName = (entity.getCareer() != null) ? entity.getCareer().getName() : null;

        return new CurriculumResponse(
                entity.getId(),
                careerId,
                careerName,
                entity.getName(),
                entity.getYear(),
                entity.getCourseCount(),
                entity.getTotalCredits(),
                entity.getStatus(),
                entity.getDescription()
        );
    }
}