package com.siladocs.application.dto;

public record CareerRequest(
        String name,
        String faculty,
        Integer cycles,
        String status
) {
}
