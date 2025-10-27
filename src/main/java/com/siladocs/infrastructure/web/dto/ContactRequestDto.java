package com.siladocs.infrastructure.web.dto;

import lombok.Getter;
import lombok.Setter;

// Asegúrate que los nombres coincidan con el JSON del frontend
@Getter
@Setter
public class ContactRequestDto {
    private String institutionName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String message;
}