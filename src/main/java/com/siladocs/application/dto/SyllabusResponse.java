package com.siladocs.application.dto;

import java.time.Instant;

public record SyllabusResponse(
    Long id,
    Long courseId,
    String courseName,
    String courseCode,
    String fileUrl,
    Long fileSize,
    String currentHash,
    String status,
    Instant uploadedAt,
    String fabricTxId
) {}
