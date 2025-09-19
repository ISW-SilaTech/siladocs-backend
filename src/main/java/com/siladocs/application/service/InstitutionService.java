package com.siladocs.application.service;

import com.siladocs.application.dto.InstitutionRequest;
import com.siladocs.application.dto.InstitutionResponse;
import com.siladocs.domain.model.Institution;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import com.siladocs.infrastructure.persistence.jparepository.InstitutionJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.InstitutionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstitutionService {

    private final InstitutionJpaRepository institutionRepo;
    private final InstitutionMapper mapper;

    public InstitutionService(InstitutionJpaRepository institutionRepo, InstitutionMapper mapper) {
        this.institutionRepo = institutionRepo;
        this.mapper = mapper;
    }

    // ---------- Crear nueva institución ----------
    @Transactional
    public InstitutionResponse createInstitution(InstitutionRequest request) {
        if (institutionRepo.existsByDomain(request.domain())) {
            throw new RuntimeException("El dominio ya está registrado");
        }

        Institution domain = new Institution(
                request.name(),
                request.domain(),
                request.status()
        );

        InstitutionEntity entity = mapper.toEntity(domain);
        InstitutionEntity saved = institutionRepo.save(entity);

        return toResponse(saved);
    }

    // ---------- Actualizar institución existente ----------
    @Transactional
    public InstitutionResponse updateInstitution(Long id, InstitutionRequest request) {
        InstitutionEntity entity = institutionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        mapper.updateEntity(entity, new Institution(request.name(), request.domain(), request.status()));
        InstitutionEntity updated = institutionRepo.save(entity);

        return toResponse(updated);
    }

    // ---------- Obtener institución por ID ----------
    public InstitutionResponse getInstitution(Long id) {
        InstitutionEntity entity = institutionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));
        return toResponse(entity);
    }

    // ---------- Listar todas las instituciones ----------
    public List<InstitutionResponse> listInstitutions() {
        return institutionRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---------- Helper para mapear a DTO ----------
    private InstitutionResponse toResponse(InstitutionEntity entity) {
        return new InstitutionResponse(
                entity.getId(),
                entity.getName(),
                entity.getDomain(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
