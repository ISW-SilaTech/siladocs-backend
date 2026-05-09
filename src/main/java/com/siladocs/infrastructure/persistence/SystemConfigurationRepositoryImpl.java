package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.SystemConfiguration;
import com.siladocs.domain.repository.SystemConfigurationRepository;
import com.siladocs.infrastructure.persistence.entity.SystemConfigurationEntity;
import com.siladocs.infrastructure.persistence.jparepository.SystemConfigurationJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.SystemConfigurationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SystemConfigurationRepositoryImpl implements SystemConfigurationRepository {

    private final SystemConfigurationJpaRepository jpaRepository;
    private final SystemConfigurationMapper mapper;

    public SystemConfigurationRepositoryImpl(SystemConfigurationJpaRepository jpaRepository, SystemConfigurationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SystemConfiguration save(SystemConfiguration config) {
        SystemConfigurationEntity entity = mapper.toEntity(config);
        SystemConfigurationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SystemConfiguration> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SystemConfiguration> findByInstitutionId(Long institutionId) {
        return jpaRepository.findByInstitutionId(institutionId).map(mapper::toDomain);
    }
}
