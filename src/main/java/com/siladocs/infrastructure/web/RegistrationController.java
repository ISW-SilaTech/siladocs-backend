package com.siladocs.infrastructure.web; // O com.siladocs.application.controller

import com.siladocs.application.dto.RegisterRequest; // 🔹 Importa el DTO correcto
import com.siladocs.application.service.AuthService; // 🔹 Inyecta AuthService
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registro") // La URL base
public class RegistrationController {

    // 🔹 Inyecta AuthService, que contiene la lógica de 'registerAdmin'
    private final AuthService authService;

    public RegistrationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/directo")
    // 🔹 Usa el DTO RegisterRequest que ya existe
    public ResponseEntity<String> registerDirectly(@RequestBody RegisterRequest request) {

        try {
            // Llama a la lógica de registro de AuthService
            authService.registerAdmin(
                    request.name(),
                    request.email(),
                    request.password(),
                    request.institutionId()
                    // Asume que institutionId SIEMPRE viene en este flujo
            );

            return ResponseEntity.status(HttpStatus.CREATED).body("Registro exitoso");

        } catch (RuntimeException e) {
            // Si el email o dominio ya existen, responde "Error"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}