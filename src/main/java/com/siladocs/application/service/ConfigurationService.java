package com.siladocs.application.service;

import com.siladocs.domain.model.Institution;
import com.siladocs.domain.model.SystemConfiguration;
import com.siladocs.domain.model.User;
import com.siladocs.domain.model.UserPreferences;
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.domain.repository.SystemConfigurationRepository;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.domain.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    private final InstitutionRepository institutionRepository;
    private final SystemConfigurationRepository systemConfigRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;

    public ConfigurationService(InstitutionRepository institutionRepository,
                              SystemConfigurationRepository systemConfigRepository,
                              UserPreferencesRepository userPreferencesRepository,
                              UserRepository userRepository) {
        this.institutionRepository = institutionRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.userRepository = userRepository;
    }

    // Institution Configuration Methods

    public Institution getInstitutionConfig(Long institutionId) {
        Optional<Institution> institution = institutionRepository.findById(institutionId);
        if (institution.isEmpty()) {
            throw new IllegalArgumentException("Institution not found with id: " + institutionId);
        }
        log.info("Retrieved institution config for id: {}", institutionId);
        return institution.get();
    }

    public Institution updateInstitutionConfig(Long institutionId, Institution updated) {
        Institution existing = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found with id: " + institutionId));

        existing.setName(updated.getName());
        existing.setDomain(updated.getDomain());

        Institution saved = institutionRepository.save(existing);
        log.info("Updated institution config for id: {}", institutionId);
        return saved;
    }

    // System Configuration Methods

    public SystemConfiguration getSystemConfig(Long institutionId) {
        return systemConfigRepository.findByInstitutionId(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("System configuration not found for institution: " + institutionId));
    }

    public SystemConfiguration updateSystemConfig(Long institutionId, SystemConfiguration updated) {
        SystemConfiguration existing = systemConfigRepository.findByInstitutionId(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("System configuration not found for institution: " + institutionId));

        existing.setMaxFileSize(updated.getMaxFileSize());
        existing.setSessionTimeout(updated.getSessionTimeout());
        existing.setEnableNotifications(updated.getEnableNotifications());
        existing.setEnableBlockchain(updated.getEnableBlockchain());
        existing.setBlockchainChannel(updated.getBlockchainChannel());
        existing.setMaxUploadRetries(updated.getMaxUploadRetries());

        SystemConfiguration saved = systemConfigRepository.save(existing);
        log.info("Updated system config for institution: {}", institutionId);
        return saved;
    }

    public SystemConfiguration createSystemConfig(Long institutionId, SystemConfiguration config) {
        config.setInstitutionId(institutionId);
        SystemConfiguration saved = systemConfigRepository.save(config);
        log.info("Created system config for institution: {}", institutionId);
        return saved;
    }

    // User Preferences Methods

    public UserPreferences getUserPreferences(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User preferences not found for user: " + userId));
    }

    public UserPreferences updateUserPreferences(Long userId, UserPreferences updated) {
        UserPreferences existing = userPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User preferences not found for user: " + userId));

        existing.setLanguage(updated.getLanguage());
        existing.setTheme(updated.getTheme());
        existing.setEmailNotifications(updated.getEmailNotifications());
        existing.setSysNotifications(updated.getSysNotifications());
        existing.setAutoSave(updated.getAutoSave());
        existing.setDateFormat(updated.getDateFormat());
        existing.setTimezone(updated.getTimezone());

        UserPreferences saved = userPreferencesRepository.save(existing);
        log.info("Updated preferences for user: {}", userId);
        return saved;
    }

    public UserPreferences createUserPreferences(Long userId, UserPreferences preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        preferences.setUserId(userId);
        UserPreferences saved = userPreferencesRepository.save(preferences);
        log.info("Created preferences for user: {}", userId);
        return saved;
    }
}
