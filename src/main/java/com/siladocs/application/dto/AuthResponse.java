package com.siladocs.application.dto;
public record AuthResponse(String token, String email, String role, Long institutionId) {}