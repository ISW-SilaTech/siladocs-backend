package com.siladocs.infrastructure.web;

import com.siladocs.application.service.ContactService;
import com.siladocs.infrastructure.web.dto.ContactRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // <-- Marca como controlador REST
@RequestMapping("/api/contact")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping // <-- Maneja peticiones POST a /api/contact
    public ResponseEntity<Void> receiveContactRequest(@RequestBody ContactRequestDto request) {
        // Validación básica (puedes añadir más con @Valid si necesitas)
        if (request == null || request.getEmail() == null || request.getInstitutionName() == null) {
            log.warn("Solicitud de contacto inválida recibida.");
            return ResponseEntity.badRequest().build();
        }

        try {
            log.info("Recibida solicitud de contacto de: {}", request.getEmail());
            contactService.processContactRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
        } catch (Exception e) {
            log.error("Error procesando solicitud de contacto de {}: {}", request.getEmail(), e.getMessage(), e); // Loguea el stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }
}