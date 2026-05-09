package com.siladocs.domain.repository;

import com.siladocs.domain.model.SystemConfiguration;
import java.util.Optional;

public interface SystemConfigurationRepository {

    SystemConfiguration save(SystemConfiguration config);
    Optional<SystemConfiguration> findById(Long id);
    Optional<SystemConfiguration> findByInstitutionId(Long institutionId);
}
