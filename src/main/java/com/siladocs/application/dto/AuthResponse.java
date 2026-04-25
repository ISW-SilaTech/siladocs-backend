package com.siladocs.application.dto;

public record AuthResponse(
    String accessToken,
    AuthUserDto user,
    AuthInstitutionDto institution
) {}

public record AuthUserDto(String id, String email, String role) {}
public record AuthInstitutionDto(String id, String name) {}
