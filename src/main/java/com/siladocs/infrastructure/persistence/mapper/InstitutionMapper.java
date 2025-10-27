package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Institution;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.stereotype.Component;

@Component
public class InstitutionMapper {

    // Convierte de Entidad (JPA) a Dominio (POJO)
    public Institution toDomain(InstitutionEntity entity) {
        if (entity == null) return null;

        return new Institution(
                entity.getInstitutionId(),
                entity.getName(),
                entity.getDomain(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    // Convierte de Dominio (POJO) a Entidad (JPA)
    public InstitutionEntity toEntity(Institution domain) {
        if (domain == null) return null;

        return new InstitutionEntity(
                domain.getInstitutionId(),
                domain.getName(),
                domain.getDomain(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    /**
     * ---- MÉTODO NUEVO ----
     * Actualiza una entidad existente (entity) con los datos
     * de un objeto de dominio (domain).
     *
     * Esto es clave para las actualizaciones en JPA:
     * 1. Traes la entidad de la BD (la original).
     * 2. Le cambias los valores (con este método).
     * 3. La guardas (JPA detecta los cambios y hace el UPDATE).
     */
    public void updateEntity(InstitutionEntity entity, Institution domain) {
        if (domain == null || entity == null) {
            return;
        }

        // Actualizamos solo los campos que pueden cambiar
        entity.setName(domain.getName());
        entity.setDomain(domain.getDomain());
        entity.setStatus(domain.getStatus());

        // NO actualizamos el 'institutionId' ni el 'createdAt'
    }
}