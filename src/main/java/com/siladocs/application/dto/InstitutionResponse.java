package com.siladocs.application.dto;

import java.time.Instant;

public record InstitutionResponse(
        Long institutionId,
        String name,
        String domain,
        String status,
        Instant createdAt
) {}
