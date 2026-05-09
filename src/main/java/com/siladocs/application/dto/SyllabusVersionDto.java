package com.siladocs.application.dto;

import java.time.Instant;

public record SyllabusVersionDto(
    Long versionId,
    Integer versionNumber,
    String fileUrl,
    String fileHash,
    String status,
    String uploadedBy,
    Instant createdAt,
    String notes,
    Boolean isOnBlockchain,
    String fabricTxId
) {}
