package com.siladocs.infrastructure.web;

import com.siladocs.application.service.ConfigurationService;
import com.siladocs.domain.model.Institution;
import com.siladocs.domain.model.SystemConfiguration;
import com.siladocs.domain.model.User;
import com.siladocs.domain.model.UserPreferences;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.web.dto.InstitutionConfigDto;
import com.siladocs.infrastructure.web.dto.SystemConfigDto;
import com.siladocs.infrastructure.web.dto.UserPreferencesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationService configurationService;
    private final UserRepository userRepository;

    public ConfigurationController(ConfigurationService configurationService, UserRepository userRepository) {
        this.configurationService = configurationService;
        this.userRepository = userRepository;
    }

    // Institution Configuration Endpoints

    @GetMapping("/institution")
    public ResponseEntity<?> getInstitutionConfig(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Institution institution = configurationService.getInstitutionConfig(user.getInstitutionId());

            InstitutionConfigDto response = new InstitutionConfigDto(
                    institution.getName(),
                    institution.getDomain(),
                    null, // email - stored in extended fields if needed
                    null, // phone
                    null  // address
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get institution config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting institution config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving institution configuration"));
        }
    }

    @PutMapping("/institution")
    public ResponseEntity<?> updateInstitutionConfig(Authentication authentication,
                                                     @RequestBody InstitutionConfigDto request) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Institution institution = new Institution(
                    user.getInstitutionId(),
                    request.getName(),
                    request.getDomain(),
                    "ACTIVE",
                    java.time.Instant.now()
            );

            Institution updated = configurationService.updateInstitutionConfig(user.getInstitutionId(), institution);

            InstitutionConfigDto response = new InstitutionConfigDto(
                    updated.getName(),
                    updated.getDomain(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getAddress()
            );

            log.info("Updated institution config for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update institution config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating institution config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating institution configuration"));
        }
    }

    // System Configuration Endpoints

    @GetMapping("/system")
    public ResponseEntity<?> getSystemConfig(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            SystemConfiguration config = configurationService.getSystemConfig(user.getInstitutionId());

            SystemConfigDto response = new SystemConfigDto(
                    config.getMaxFileSize(),
                    config.getSessionTimeout(),
                    config.getEnableNotifications(),
                    config.getEnableBlockchain(),
                    config.getBlockchainChannel(),
                    config.getMaxUploadRetries()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get system config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting system config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving system configuration"));
        }
    }

    @PutMapping("/system")
    public ResponseEntity<?> updateSystemConfig(Authentication authentication,
                                               @RequestBody SystemConfigDto request) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            SystemConfiguration config = new SystemConfiguration(
                    null,
                    user.getInstitutionId(),
                    request.getMaxFileSize(),
                    request.getSessionTimeout(),
                    request.getEnableNotifications(),
                    request.getEnableBlockchain(),
                    request.getBlockchainChannel(),
                    request.getMaxUploadRetries(),
                    null,
                    null
            );

            SystemConfiguration updated = configurationService.updateSystemConfig(user.getInstitutionId(), config);

            SystemConfigDto response = new SystemConfigDto(
                    updated.getMaxFileSize(),
                    updated.getSessionTimeout(),
                    updated.getEnableNotifications(),
                    updated.getEnableBlockchain(),
                    updated.getBlockchainChannel(),
                    updated.getMaxUploadRetries()
            );

            log.info("Updated system config for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update system config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating system config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating system configuration"));
        }
    }

    // User Preferences Endpoints

    @GetMapping("/preferences")
    public ResponseEntity<?> getUserPreferences(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            UserPreferences prefs = configurationService.getUserPreferences(user.getUserId());

            UserPreferencesDto response = new UserPreferencesDto(
                    prefs.getLanguage(),
                    prefs.getTheme(),
                    prefs.getEmailNotifications(),
                    prefs.getSysNotifications(),
                    prefs.getAutoSave(),
                    prefs.getDateFormat(),
                    prefs.getTimezone()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get user preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting user preferences: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving user preferences"));
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updateUserPreferences(Authentication authentication,
                                                  @RequestBody UserPreferencesDto request) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            UserPreferences prefs = new UserPreferences(
                    null,
                    user.getUserId(),
                    request.getLanguage(),
                    request.getTheme(),
                    request.getEmailNotifications(),
                    request.getSysNotifications(),
                    request.getAutoSave(),
                    request.getDateFormat(),
                    request.getTimezone(),
                    null,
                    null
            );

            UserPreferences updated = configurationService.updateUserPreferences(user.getUserId(), prefs);

            UserPreferencesDto response = new UserPreferencesDto(
                    updated.getLanguage(),
                    updated.getTheme(),
                    updated.getEmailNotifications(),
                    updated.getSysNotifications(),
                    updated.getAutoSave(),
                    updated.getDateFormat(),
                    updated.getTimezone()
            );

            log.info("Updated preferences for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update user preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user preferences: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating user preferences"));
        }
    }
}
