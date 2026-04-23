package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.Institution;
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import com.siladocs.infrastructure.persistence.jparepository.InstitutionJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.InstitutionMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InstitutionRepositoryImpl implements InstitutionRepository {

    private final InstitutionJpaRepository jpaRepository;
    private final InstitutionMapper mapper;

    public InstitutionRepositoryImpl(InstitutionJpaRepository jpaRepository, InstitutionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Institution save(Institution institution) {
        InstitutionEntity entity = mapper.toEntity(institution);
        InstitutionEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    // ⬇️ CORRECCIÓN: Buscamos el Entity y lo pasamos por el Mapper
    @Override
    public Institution findByName(String name) {
        InstitutionEntity entity = jpaRepository.findByName(name);
        
        if (entity == null) {
            return null; // O lanza una excepción si tu lógica de dominio lo prefiere
        }
        
        // Usamos tu mapper para traducirlo
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Institution> findByDomain(String domain) {
        Optional<InstitutionEntity> entity = jpaRepository.findByDomain(domain);
        return entity.map(mapper::toDomain);
    }
}
