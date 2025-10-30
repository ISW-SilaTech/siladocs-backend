package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.UserEntity;
import com.siladocs.infrastructure.persistence.jparepository.UserJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryImpl(UserJpaRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        // Convierte la entidad de JPA a dominio
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        // 1. Convierte de dominio a entidad
        UserEntity entity = mapper.toEntity(user);
        // 2. Guarda la entidad
        UserEntity savedEntity = jpaRepository.save(entity);
        // 3. Devuelve el modelo de dominio
        return mapper.toDomain(savedEntity);
    }

    // --- 🔹 MÉTODO ACTUALIZADO 🔹 ---
    @Override
    public Optional<User> findById(Long userId) {
        // 1. Busca la entidad JPA por ID
        Optional<UserEntity> entityOptional = jpaRepository.findById(userId);
        // 2. Mapea el resultado (si existe) de Entidad a Dominio
        return entityOptional.map(mapper::toDomain);
    }
}