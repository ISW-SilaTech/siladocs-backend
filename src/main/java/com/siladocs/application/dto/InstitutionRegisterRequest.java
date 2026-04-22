package com.siladocs.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionRegisterRequest(

        @NotBlank(message = "El código de acceso es obligatorio")
        String accessCode,

        @NotBlank(message = "El nombre completo es obligatorio")
        String fullName,

        @Email(message = "Email inválido")
        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password

) {}
