package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Syllabus; // 🔹 (Necesitas crear domain.model.Syllabus)
import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import org.springframework.stereotype.Component;

@Component
public class SyllabusMapper {

    // Convierte de Entidad (JPA) a Dominio
    public Syllabus toDomain(SyllabusEntity entity) {
        if (entity == null) return null;

        return new Syllabus(
                entity.getId(),
                entity.getCourse() != null ? entity.getCourse().getId() : null,
                entity.getCurrentVersion(),
                entity.getStatus(),
                entity.getFileUrl(),
                entity.getCurrentHash(),
                entity.getLastChainHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // Convierte de Dominio a Entidad (JPA)
    // Nota: 'course' se pasa por separado para establecer la relación
    public SyllabusEntity toEntity(Syllabus domain, CourseEntity course) {
        if (domain == null || course == null) return null;

        SyllabusEntity entity = new SyllabusEntity();
        entity.setId(domain.getId()); // Para actualizaciones
        entity.setCourse(course);
        entity.setCurrentVersion(domain.getCurrentVersion());
        entity.setStatus(domain.getStatus());
        entity.setFileUrl(domain.getFileUrl());
        entity.setCurrentHash(domain.getCurrentHash());
        entity.setLastChainHash(domain.getLastChainHash());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}