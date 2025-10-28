package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Career;
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import org.springframework.stereotype.Component;
import java.time.LocalDate; // O Instant

@Component
public class CareerMapper {

    public Career toDomain(CareerEntity entity) {
        if (entity == null) return null;
        return new Career(
                entity.getId(),
                entity.getName(),
                entity.getFaculty(),
                entity.getCycles(),
                entity.getLastUpdated(),
                entity.getStatus()
        );
    }

    public CareerEntity toEntity(Career domain) {
        if (domain == null) return null;
        CareerEntity entity = new CareerEntity();
        entity.setId(domain.getId()); // Importante para actualizaciones
        entity.setName(domain.getName());
        entity.setFaculty(domain.getFaculty());
        entity.setCycles(domain.getCycles());
        // lastUpdated se maneja usualmente en el servicio antes de guardar
        entity.setLastUpdated(domain.getLastUpdated() != null ? domain.getLastUpdated() : LocalDate.now());
        entity.setStatus(domain.getStatus());
        return entity;
    }

    // Método para actualizar una entidad existente (evita crear una nueva)
    public void updateEntity(CareerEntity entity, Career domain) {
        if (domain == null || entity == null) return;
        entity.setName(domain.getName());
        entity.setFaculty(domain.getFaculty());
        entity.setCycles(domain.getCycles());
        entity.setStatus(domain.getStatus());
        entity.setLastUpdated(LocalDate.now()); // Siempre actualiza la fecha al modificar
    }
}