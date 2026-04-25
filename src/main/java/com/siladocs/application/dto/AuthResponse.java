package com.siladocs.application.dto;

public record AuthResponse(
    String accessToken,
    AuthUserDto user,
    AuthInstitutionDto institution
) {}

record AuthUserDto(String id, String email, String role) {}
record AuthInstitutionDto(String id, String name) {}
