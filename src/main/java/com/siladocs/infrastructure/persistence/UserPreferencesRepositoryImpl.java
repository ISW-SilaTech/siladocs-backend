package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.UserPreferences;
import com.siladocs.domain.repository.UserPreferencesRepository;
import com.siladocs.infrastructure.persistence.entity.UserPreferencesEntity;
import com.siladocs.infrastructure.persistence.jparepository.UserPreferencesJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.UserPreferencesMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserPreferencesRepositoryImpl implements UserPreferencesRepository {

    private final UserPreferencesJpaRepository jpaRepository;
    private final UserPreferencesMapper mapper;

    public UserPreferencesRepositoryImpl(UserPreferencesJpaRepository jpaRepository, UserPreferencesMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
        UserPreferencesEntity entity = mapper.toEntity(preferences);
        UserPreferencesEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserPreferences> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserPreferences> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }
}
