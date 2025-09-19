package com.siladocs.application.dto;
public record RegisterRequest(String name, String email, String password, Long institutionId) {}