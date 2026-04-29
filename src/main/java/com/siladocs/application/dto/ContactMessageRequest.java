package com.siladocs.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    String name,

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    String email,

    @NotBlank(message = "El asunto es requerido")
    @Size(min = 2, max = 255, message = "El asunto debe tener entre 2 y 255 caracteres")
    String subject,

    @NotBlank(message = "El mensaje es requerido")
    @Size(min = 10, max = 5000, message = "El mensaje debe tener entre 10 y 5000 caracteres")
    String message,

    @Size(max = 20, message = "El teléfono debe tener máximo 20 caracteres")
    String phone,

    @Size(max = 255, message = "La empresa debe tener máximo 255 caracteres")
    String company,

    @NotBlank(message = "El token de reCAPTCHA es requerido")
    String recaptchaToken
) {}
