package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class UserPreferences {

    private Long preferenceId;
    private Long userId;
    private String language;
    private String theme;
    private Boolean emailNotifications;
    private Boolean sysNotifications;
    private Boolean autoSave;
    private String dateFormat;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;

    public UserPreferences(Long userId, String language, String theme,
                         Boolean emailNotifications, Boolean sysNotifications,
                         Boolean autoSave, String dateFormat, String timezone) {
        this.userId = userId;
        this.language = language;
        this.theme = theme;
        this.emailNotifications = emailNotifications;
        this.sysNotifications = sysNotifications;
        this.autoSave = autoSave;
        this.dateFormat = dateFormat;
        this.timezone = timezone;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
