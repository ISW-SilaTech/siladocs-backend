package com.siladocs.application.dto;

public record AuthResponse(
    String accessToken,
    AuthUserDto user,
    AuthInstitutionDto institution
) {}
