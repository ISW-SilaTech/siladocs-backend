package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.UserPreferences;
import com.siladocs.infrastructure.persistence.entity.UserPreferencesEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesMapper {

    public UserPreferences toDomain(UserPreferencesEntity entity) {
        if (entity == null) return null;

        return new UserPreferences(
                entity.getPreferenceId(),
                entity.getUserId(),
                entity.getLanguage(),
                entity.getTheme(),
                entity.getEmailNotifications(),
                entity.getSysNotifications(),
                entity.getAutoSave(),
                entity.getDateFormat(),
                entity.getTimezone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserPreferencesEntity toEntity(UserPreferences domain) {
        if (domain == null) return null;

        return new UserPreferencesEntity(
                domain.getPreferenceId(),
                domain.getUserId(),
                domain.getLanguage(),
                domain.getTheme(),
                domain.getEmailNotifications(),
                domain.getSysNotifications(),
                domain.getAutoSave(),
                domain.getDateFormat(),
                domain.getTimezone(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public void updateEntity(UserPreferencesEntity entity, UserPreferences domain) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setLanguage(domain.getLanguage());
        entity.setTheme(domain.getTheme());
        entity.setEmailNotifications(domain.getEmailNotifications());
        entity.setSysNotifications(domain.getSysNotifications());
        entity.setAutoSave(domain.getAutoSave());
        entity.setDateFormat(domain.getDateFormat());
        entity.setTimezone(domain.getTimezone());
        entity.setUpdatedAt(java.time.Instant.now());
    }
}
