package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.Institution;
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import com.siladocs.infrastructure.persistence.jparepository.InstitutionJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.InstitutionMapper;
import org.springframework.stereotype.Repository; // <-- 1. Importa la anotación

import java.util.Optional;

@Repository // <-- 2. Añade la anotación aquí
public class InstitutionRepositoryImpl implements InstitutionRepository {

    private final InstitutionJpaRepository jpaRepository;
    private final InstitutionMapper mapper;

    public InstitutionRepositoryImpl(InstitutionJpaRepository jpaRepository, InstitutionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    // ... (el resto de tus métodos save() y findByDomain() van aquí) ...
    @Override
    public Institution save(Institution institution) {
        InstitutionEntity entity = mapper.toEntity(institution);
        InstitutionEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Institution> findByDomain(String domain) {
        Optional<InstitutionEntity> entity = jpaRepository.findByDomain(domain);
        return entity.map(mapper::toDomain);
    }
}