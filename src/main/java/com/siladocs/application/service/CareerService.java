package com.siladocs.application.service;

import com.siladocs.application.dto.CareerRequest;
import com.siladocs.application.dto.CareerResponse;
import com.siladocs.domain.model.Career; // Modelo de dominio (si lo usas internamente)
import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import com.siladocs.infrastructure.persistence.jparepository.CareerJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.CareerMapper; // Mapper si lo usas
import jakarta.persistence.EntityNotFoundException; // Para errores
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate; // O Instant
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareerService {

    private static final Logger log = LoggerFactory.getLogger(CareerService.class);

    private final CareerJpaRepository careerRepository;
    // Inyecta el Mapper si decides usarlo para convertir DTOs
    // private final CareerMapper careerMapper;

    // Constructor con dependencias
    public CareerService(CareerJpaRepository careerRepository /*, CareerMapper careerMapper */) {
        this.careerRepository = careerRepository;
        // this.careerMapper = careerMapper;
    }

    // --- Operaciones CRUD ---

    @Transactional
    public CareerResponse createCareer(CareerRequest request) {
        log.info("Creando carrera con nombre: {}", request.name());
        if (careerRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Ya existe una carrera con el nombre: " + request.name());
        }

        CareerEntity entity = new CareerEntity();
        entity.setName(request.name());
        entity.setFaculty(request.faculty());
        entity.setCycles(request.cycles());
        entity.setStatus(request.status());
        entity.setLastUpdated(LocalDate.now()); // O Instant.now()

        CareerEntity savedEntity = careerRepository.save(entity);
        log.info("Carrera creada con ID: {}", savedEntity.getId());
        return entityToResponse(savedEntity); // Usa el helper DTO
    }

    @Transactional(readOnly = true)
    public CareerResponse getCareerById(Long id) {
        log.debug("Buscando carrera con ID: {}", id);
        CareerEntity entity = careerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada con ID: " + id));
        return entityToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CareerResponse> getAllCareers() {
        log.debug("Listando todas las carreras");
        return careerRepository.findAll()
                .stream()
                .map(this::entityToResponse) // Usa el helper DTO
                .collect(Collectors.toList());
    }

    @Transactional
    public CareerResponse updateCareer(Long id, CareerRequest request) {
        log.info("Actualizando carrera con ID: {}", id);
        CareerEntity existingEntity = careerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada con ID: " + id));

        // Opcional: Validar si el nuevo nombre ya existe (y no es el mismo registro)
        // careerRepository.findByName(request.name()).ifPresent(found -> {
        //     if (!found.getId().equals(id)) {
        //         throw new IllegalArgumentException("Ya existe otra carrera con el nombre: " + request.name());
        //     }
        // });

        existingEntity.setName(request.name());
        existingEntity.setFaculty(request.faculty());
        existingEntity.setCycles(request.cycles());
        existingEntity.setStatus(request.status());
        existingEntity.setLastUpdated(LocalDate.now()); // Actualiza fecha

        CareerEntity updatedEntity = careerRepository.save(existingEntity);
        log.info("Carrera actualizada con ID: {}", updatedEntity.getId());
        return entityToResponse(updatedEntity);
    }

    @Transactional
    public void deleteCareer(Long id) {
        log.warn("Eliminando carrera con ID: {}", id); // Usar WARN para operaciones destructivas
        if (!careerRepository.existsById(id)) {
            throw new EntityNotFoundException("Carrera no encontrada con ID: " + id);
        }
        // Considera si realmente quieres borrar o solo cambiar el estado a "Inactivo"
        // Si tienes Mallas asociadas, borrarlas podría dar error si no configuras Cascade
        careerRepository.deleteById(id);
        log.info("Carrera eliminada con ID: {}", id);
    }

    // --- Helper para convertir Entity a Response DTO ---
    // (Mover a un Mapper si prefieres)
    private CareerResponse entityToResponse(CareerEntity entity) {
        return new CareerResponse(
                entity.getId(),
                entity.getName(),
                entity.getFaculty(),
                entity.getCycles(),
                entity.getLastUpdated(),
                entity.getStatus()
        );
    }
}