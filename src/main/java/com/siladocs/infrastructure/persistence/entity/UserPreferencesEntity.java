package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_preferences")
public class UserPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 10)
    private String language;

    @Column(length = 20)
    private String theme;

    @Column(name = "email_notifications")
    private Boolean emailNotifications;

    @Column(name = "sys_notifications")
    private Boolean sysNotifications;

    @Column(name = "auto_save")
    private Boolean autoSave;

    @Column(name = "date_format")
    private String dateFormat;

    @Column(length = 50)
    private String timezone;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
