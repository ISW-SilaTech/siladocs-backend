package com.siladocs.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContactMessageListDto(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("name")
    String name,

    @JsonProperty("email")
    String email,

    @JsonProperty("subject")
    String subject,

    @JsonProperty("status")
    String status,

    @JsonProperty("createdAt")
    LocalDateTime createdAt,

    @JsonProperty("unread")
    boolean unread
) {
    public static ContactMessageListDto fromDomain(com.siladocs.domain.model.ContactMessage domain) {
        return new ContactMessageListDto(
            domain.getId(),
            domain.getName(),
            domain.getEmail(),
            domain.getSubject(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            "NEW".equals(domain.getStatus().name())
        );
    }
}
