package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Curriculum;
import com.siladocs.infrastructure.persistence.entity.CareerEntity; // Need this for relationship
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import org.springframework.stereotype.Component;

@Component
public class CurriculumMapper {

    // Entity -> Domain
    public Curriculum toDomain(CurriculumEntity entity) {
        if (entity == null) return null;
        return new Curriculum(
                entity.getId(),
                entity.getCareer() != null ? entity.getCareer().getId() : null, // Get career ID
                entity.getName(),
                entity.getYear(),
                entity.getCourseCount(),
                entity.getTotalCredits(),
                entity.getStatus(),
                entity.getDescription()
        );
    }

    // Domain -> Entity (Requires fetched CareerEntity)
    public CurriculumEntity toEntity(Curriculum domain, CareerEntity careerEntity) {
        if (domain == null || careerEntity == null) return null;
        CurriculumEntity entity = new CurriculumEntity();
        entity.setId(domain.getId()); // Needed for updates
        entity.setName(domain.getName());
        entity.setYear(domain.getYear());
        entity.setCourseCount(domain.getCourseCount());
        entity.setTotalCredits(domain.getTotalCredits());
        entity.setStatus(domain.getStatus());
        entity.setDescription(domain.getDescription());
        entity.setCareer(careerEntity); // Set the relationship
        return entity;
    }

    // Update existing Entity from Domain (Requires fetched CareerEntity)
    public void updateEntity(CurriculumEntity entity, Curriculum domain, CareerEntity careerEntity) {
        if (entity == null || domain == null || careerEntity == null) return;
        entity.setName(domain.getName());
        entity.setYear(domain.getYear());
        entity.setCourseCount(domain.getCourseCount());
        entity.setTotalCredits(domain.getTotalCredits());
        entity.setStatus(domain.getStatus());
        entity.setDescription(domain.getDescription());
        entity.setCareer(careerEntity); // Update relationship if needed (though usually ID doesn't change)
    }
}