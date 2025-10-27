package com.siladocs.infrastructure.web.dto;

import lombok.Getter;
import lombok.Setter;

// Este es el "paquete" (Data Transfer Object)
// que transporta los datos del JSON del frontend
@Getter
@Setter
public class DirectRegistrationRequest {
    private String nombreInstitucion;
    private String domain;
    private String nombreAdmin;
    private String email;
    private String password;
}