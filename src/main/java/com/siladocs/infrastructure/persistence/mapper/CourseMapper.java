package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Course;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class CourseMapper {

    // Entity -> Domain
    public Course toDomain(CourseEntity entity) {
        if (entity == null) return null;
        return new Course(
                entity.getId(),
                entity.getCurriculum() != null ? entity.getCurriculum().getId() : null,
                entity.getCareer() != null ? entity.getCareer().getId() : null,
                entity.getCode(),
                entity.getName(),
                entity.getFaculty(),
                entity.getSyllabusCount(),
                entity.getYear(),
                entity.getStatus(),
                entity.getPublicationDate()
        );
    }

    // Domain -> Entity (Requires fetched CurriculumEntity and CareerEntity)
    public CourseEntity toEntity(Course domain, CurriculumEntity curriculum, CareerEntity career) {
        if (domain == null || curriculum == null || career == null) return null;
        CourseEntity entity = new CourseEntity();
        entity.setId(domain.getId()); // For updates
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setFaculty(domain.getFaculty()); // Or derive from career
        entity.setSyllabusCount(domain.getSyllabusCount() != null ? domain.getSyllabusCount() : 0);
        entity.setYear(domain.getYear());
        entity.setStatus(domain.getStatus());
        entity.setPublicationDate(domain.getPublicationDate());
        entity.setCurriculum(curriculum); // Set relationships
        entity.setCareer(career);
        return entity;
    }

    // Update existing Entity
    public void updateEntity(CourseEntity entity, Course domain, CurriculumEntity curriculum, CareerEntity career) {
        if (entity == null || domain == null || curriculum == null || career == null) return;
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setFaculty(domain.getFaculty());
        entity.setSyllabusCount(domain.getSyllabusCount() != null ? domain.getSyllabusCount() : 0);
        entity.setYear(domain.getYear());
        entity.setStatus(domain.getStatus());
        entity.setPublicationDate(domain.getPublicationDate());
        entity.setCurriculum(curriculum);
        entity.setCareer(career);
    }
}