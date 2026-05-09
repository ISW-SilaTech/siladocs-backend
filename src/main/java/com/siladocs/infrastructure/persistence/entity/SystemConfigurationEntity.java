package com.siladocs.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_configuration")
public class SystemConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "max_file_size")
    private Integer maxFileSize;

    @Column(name = "session_timeout")
    private Integer sessionTimeout;

    @Column(name = "enable_notifications")
    private Boolean enableNotifications;

    @Column(name = "enable_blockchain")
    private Boolean enableBlockchain;

    @Column(name = "blockchain_channel")
    private String blockchainChannel;

    @Column(name = "max_upload_retries")
    private Integer maxUploadRetries;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
