package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.CareerRequest;
import com.siladocs.application.dto.CareerResponse;
import com.siladocs.application.service.CareerService;
import jakarta.persistence.EntityNotFoundException; // Para manejar errores 404
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers") // URL base para carreras
public class CareerController {

    private static final Logger log = LoggerFactory.getLogger(CareerController.class);
    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    // POST /api/careers - Crear nueva carrera
    @PostMapping
    public ResponseEntity<?> createCareer(@RequestBody CareerRequest request) {
        try {
            CareerResponse response = careerService.createCareer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Intento de crear carrera duplicada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict
        } catch (Exception e) {
            log.error("Error al crear carrera: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al crear la carrera.");
        }
    }

    // GET /api/careers/{id} - Obtener carrera por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCareerById(@PathVariable Long id) {
        try {
            CareerResponse response = careerService.getCareerById(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Carrera no encontrada con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404 Not Found
        } catch (Exception e) {
            log.error("Error al obtener carrera con ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al obtener la carrera.");
        }
    }

    // GET /api/careers - Obtener todas las carreras
    @GetMapping
    public ResponseEntity<List<CareerResponse>> getAllCareers() {
        try {
            List<CareerResponse> responseList = careerService.getAllCareers();
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error al listar carreras: {}", e.getMessage(), e);
            // Considera devolver una lista vacía o un error 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // PUT /api/careers/{id} - Actualizar carrera
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCareer(@PathVariable Long id, @RequestBody CareerRequest request) {
        try {
            CareerResponse response = careerService.updateCareer(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Intento de actualizar carrera no encontrada con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404 Not Found
        } catch (IllegalArgumentException e) {
            log.warn("Conflicto al actualizar carrera con ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict (si validas nombre duplicado)
        } catch (Exception e) {
            log.error("Error al actualizar carrera con ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al actualizar la carrera.");
        }
    }

    // DELETE /api/careers/{id} - Eliminar carrera
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        try {
            careerService.deleteCareer(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            log.info("Intento de eliminar carrera no encontrada con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        } catch (Exception e) { // Podría ser DataIntegrityViolation si hay Mallas asociadas
            log.error("Error al eliminar carrera con ID {}: {}", id, e.getMessage(), e);
            // Devolver 409 Conflict si hay dependencias podría ser más informativo
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}