package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Institution;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.stereotype.Component;

@Component
public class InstitutionMapper {

    // Convierte de Entity a Domain
    public Institution toDomain(InstitutionEntity entity) {
        if (entity == null) return null;
        return new Institution(
                entity.getName(),
                entity.getDomain(),
                entity.getStatus()
        );
    }

    // Convierte de Domain a Entity
    public InstitutionEntity toEntity(Institution domain) {
        if (domain == null) return null;
        return new InstitutionEntity(
                domain.getName(),
                domain.getDomain(),
                domain.getStatus()
        );
    }

    // Actualiza una entidad existente con datos del dominio
    public void updateEntity(InstitutionEntity entity, Institution domain) {
        entity.setName(domain.getName());
        entity.setDomain(domain.getDomain());
        entity.setStatus(domain.getStatus());
    }
}
