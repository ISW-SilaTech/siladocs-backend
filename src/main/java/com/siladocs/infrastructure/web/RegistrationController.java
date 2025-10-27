package com.siladocs.infrastructure.web;

import com.siladocs.application.service.RegistrationService;
import com.siladocs.infrastructure.web.dto.DirectRegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registro") // La URL base
public class RegistrationController {

    private final RegistrationService registrationService;

    // Inyectamos el "cerebro" (el servicio)
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // Esta es la puerta de entrada para tu frontend
    @PostMapping("/directo")
    public ResponseEntity<String> registerDirectly(@RequestBody DirectRegistrationRequest request) {

        try {
            // 1. Llama al servicio con los datos del "paquete" (DTO)
            registrationService.registerNewInstitution(
                    request.getNombreInstitucion(),
                    request.getDomain(),
                    request.getNombreAdmin(),
                    request.getEmail(),
                    request.getPassword()
            );

            // 2. Si todo sale bien, responde "Éxito"
            return ResponseEntity.status(HttpStatus.CREATED).body("Registro exitoso");

        } catch (IllegalStateException e) {
            // 3. Si el email o dominio ya existen, responde "Error"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}