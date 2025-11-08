package com.siladocs.application.dto;

import java.time.Instant;

// Este DTO representa un "bloque" en el historial para el frontend
public record SyllabusHistoryResponse(
        long version,
        String dataHash,
        Instant timestamp,
        String actorEmail,
        String action
) {}