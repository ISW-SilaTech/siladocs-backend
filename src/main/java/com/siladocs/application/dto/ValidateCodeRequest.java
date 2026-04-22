package com.siladocs.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateCodeRequest(

        @NotBlank(message = "El código es obligatorio")
        String code

) {}
