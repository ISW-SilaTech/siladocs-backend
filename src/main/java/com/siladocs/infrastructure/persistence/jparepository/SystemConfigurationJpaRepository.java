package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.SystemConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationJpaRepository extends JpaRepository<SystemConfigurationEntity, Long> {

    Optional<SystemConfigurationEntity> findByInstitutionId(Long institutionId);
}
