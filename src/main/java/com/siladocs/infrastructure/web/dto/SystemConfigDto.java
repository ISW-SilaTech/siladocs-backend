package com.siladocs.infrastructure.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDto {

    private Integer maxFileSize;
    private Integer sessionTimeout;
    private Boolean enableNotifications;
    private Boolean enableBlockchain;
    private String blockchainChannel;
    private Integer maxUploadRetries;
}
