package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.SystemConfiguration;
import com.siladocs.infrastructure.persistence.entity.SystemConfigurationEntity;
import org.springframework.stereotype.Component;

@Component
public class SystemConfigurationMapper {

    public SystemConfiguration toDomain(SystemConfigurationEntity entity) {
        if (entity == null) return null;

        return new SystemConfiguration(
                entity.getConfigId(),
                entity.getInstitutionId(),
                entity.getMaxFileSize(),
                entity.getSessionTimeout(),
                entity.getEnableNotifications(),
                entity.getEnableBlockchain(),
                entity.getBlockchainChannel(),
                entity.getMaxUploadRetries(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SystemConfigurationEntity toEntity(SystemConfiguration domain) {
        if (domain == null) return null;

        return new SystemConfigurationEntity(
                domain.getConfigId(),
                domain.getInstitutionId(),
                domain.getMaxFileSize(),
                domain.getSessionTimeout(),
                domain.getEnableNotifications(),
                domain.getEnableBlockchain(),
                domain.getBlockchainChannel(),
                domain.getMaxUploadRetries(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public void updateEntity(SystemConfigurationEntity entity, SystemConfiguration domain) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setMaxFileSize(domain.getMaxFileSize());
        entity.setSessionTimeout(domain.getSessionTimeout());
        entity.setEnableNotifications(domain.getEnableNotifications());
        entity.setEnableBlockchain(domain.getEnableBlockchain());
        entity.setBlockchainChannel(domain.getBlockchainChannel());
        entity.setMaxUploadRetries(domain.getMaxUploadRetries());
        entity.setUpdatedAt(java.time.Instant.now());
    }
}
