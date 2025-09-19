package com.siladocs.application.controller;

import com.siladocs.application.dto.InstitutionRequest;
import com.siladocs.application.dto.InstitutionResponse;
import com.siladocs.application.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/institutions")
@Tag(name = "Institutions", description = "Endpoints para gestión de instituciones")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    // ---------- Crear institución ----------
    @PostMapping
    @Operation(summary = "Crear nueva institución", description = "Crea una nueva institución")
    public ResponseEntity<InstitutionResponse> createInstitution(@RequestBody InstitutionRequest request) {
        try {
            InstitutionResponse response = institutionService.createInstitution(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // ---------- Actualizar institución ----------
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar institución", description = "Actualiza los datos de una institución existente")
    public ResponseEntity<InstitutionResponse> updateInstitution(
            @PathVariable Long id,
            @RequestBody InstitutionRequest request
    ) {
        try {
            InstitutionResponse response = institutionService.updateInstitution(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ---------- Obtener institución por ID ----------
    @GetMapping("/{id}")
    @Operation(summary = "Obtener institución", description = "Devuelve la institución por ID")
    public ResponseEntity<InstitutionResponse> getInstitution(@PathVariable Long id) {
        try {
            InstitutionResponse response = institutionService.getInstitution(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ---------- Listar todas las instituciones ----------
    @GetMapping
    @Operation(summary = "Listar instituciones", description = "Devuelve todas las instituciones")
    public ResponseEntity<List<InstitutionResponse>> listInstitutions() {
        List<InstitutionResponse> institutions = institutionService.listInstitutions();
        return ResponseEntity.ok(institutions);
    }
}
