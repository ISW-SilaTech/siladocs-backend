package com.siladocs.application.dto;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String fileName,
        String fileType,
        Long fileSize,
        String hash,
        Instant uploadedAt
) {}
