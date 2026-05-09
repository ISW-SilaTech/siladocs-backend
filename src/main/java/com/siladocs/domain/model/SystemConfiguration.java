package com.siladocs.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class SystemConfiguration {

    private Long configId;
    private Long institutionId;
    private Integer maxFileSize;
    private Integer sessionTimeout;
    private Boolean enableNotifications;
    private Boolean enableBlockchain;
    private String blockchainChannel;
    private Integer maxUploadRetries;
    private Instant createdAt;
    private Instant updatedAt;

    public SystemConfiguration(Long institutionId, Integer maxFileSize, Integer sessionTimeout,
                             Boolean enableNotifications, Boolean enableBlockchain,
                             String blockchainChannel, Integer maxUploadRetries) {
        this.institutionId = institutionId;
        this.maxFileSize = maxFileSize;
        this.sessionTimeout = sessionTimeout;
        this.enableNotifications = enableNotifications;
        this.enableBlockchain = enableBlockchain;
        this.blockchainChannel = blockchainChannel;
        this.maxUploadRetries = maxUploadRetries;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
