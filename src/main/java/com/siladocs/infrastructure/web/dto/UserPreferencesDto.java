package com.siladocs.infrastructure.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDto {

    private String language;
    private String theme;
    private Boolean emailNotifications;
    private Boolean sysNotifications;
    private Boolean autoSave;
    private String dateFormat;
    private String timezone;
}
