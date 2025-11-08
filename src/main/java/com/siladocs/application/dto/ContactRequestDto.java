package com.siladocs.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequestDto {
    // Nombres deben coincidir con el JSON enviado por el frontend
    private String institutionName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String message;
}