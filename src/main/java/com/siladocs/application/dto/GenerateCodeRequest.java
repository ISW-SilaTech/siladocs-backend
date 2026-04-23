package com.siladocs.application.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateCodeRequest(
    @NotBlank String institutionName
) {}
