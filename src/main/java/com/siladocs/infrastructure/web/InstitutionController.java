package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.InstitutionRequest;
import com.siladocs.application.dto.InstitutionResponse;
import com.siladocs.application.service.InstitutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions") // ⬅️ ¡Aquí está la URL base!
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    // Endpoint para CREAR: POST /api/institutions
    @PostMapping
    public ResponseEntity<InstitutionResponse> create(@RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.createInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint para LEER (UNO): GET /api/institutions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<InstitutionResponse> getById(@PathVariable Long id) {
        InstitutionResponse response = institutionService.getInstitution(id);
        return ResponseEntity.ok(response);
    }

    // Endpoint para LEER (TODOS): GET /api/institutions
    @GetMapping
    public ResponseEntity<List<InstitutionResponse>> getAll() {
        List<InstitutionResponse> responseList = institutionService.listInstitutions();
        return ResponseEntity.ok(responseList);
    }

    // Endpoint para ACTUALIZAR: PUT /api/institutions/{id}
    @PutMapping("/{id}")
    public ResponseEntity<InstitutionResponse> update(@PathVariable Long id, @RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.updateInstitution(id, request);
        return ResponseEntity.ok(response);
    }
}