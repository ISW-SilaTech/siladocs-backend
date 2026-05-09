package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesJpaRepository extends JpaRepository<UserPreferencesEntity, Long> {

    Optional<UserPreferencesEntity> findByUserId(Long userId);
}
