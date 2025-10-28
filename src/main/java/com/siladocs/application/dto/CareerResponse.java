package com.siladocs.application.dto;

import java.time.LocalDate; // O Instant


public record CareerResponse(
        Long id,
        String name,
        String faculty,
        Integer cycles,
        LocalDate lastUpdated, // O Instant
        String status
) {}

// Helper para convertir Entity a Response DTO dentro del servicio o mapper
// (Puedes poner este método estático en CareerMapper o crear un DtoMapper)
/* public static CareerResponse entityToResponse(CareerEntity entity) {
    return new CareerResponse(
        entity.getId(),
        entity.getName(),
        entity.getFaculty(),
        entity.getCycles(),
        entity.getLastUpdated(),
        entity.getStatus()
    );
}
*/