package com.siladocs.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContactMessageResponse(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("name")
    String name,

    @JsonProperty("email")
    String email,

    @JsonProperty("subject")
    String subject,

    @JsonProperty("message")
    String message,

    @JsonProperty("phone")
    String phone,

    @JsonProperty("company")
    String company,

    @JsonProperty("status")
    String status,

    @JsonProperty("createdAt")
    LocalDateTime createdAt,

    @JsonProperty("updatedAt")
    LocalDateTime updatedAt,

    @JsonProperty("ticketId")
    String ticketId
) {
    public static ContactMessageResponse fromDomain(com.siladocs.domain.model.ContactMessage domain) {
        String ticketId = String.format("TKT-%d-%d", domain.getId().hashCode(), System.currentTimeMillis() % 10000);

        return new ContactMessageResponse(
            domain.getId(),
            domain.getName(),
            domain.getEmail(),
            domain.getSubject(),
            domain.getMessage(),
            domain.getPhone(),
            domain.getCompany(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            ticketId
        );
    }
}
