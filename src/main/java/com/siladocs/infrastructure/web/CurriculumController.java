package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.CurriculumRequest;
import com.siladocs.application.dto.CurriculumResponse;
import com.siladocs.application.service.CurriculumService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/curriculums") // Base URL for curriculums
public class CurriculumController {

    private static final Logger log = LoggerFactory.getLogger(CurriculumController.class);
    // HU: solo Administrador Académico puede crear/editar/eliminar mallas.
    private static final String ADMIN_ROLE = "Administrador Académico";
    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ADMIN_ROLE.equals(a.getAuthority()));
    }

    // POST /api/curriculums - Create new curriculum
    @PostMapping
    public ResponseEntity<?> createCurriculum(Authentication authentication, @RequestBody CurriculumRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede crear mallas curriculares.");
        }
        try {
            CurriculumResponse response = curriculumService.createCurriculum(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            log.warn("Failed to create curriculum: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 400 for bad input/refs
        } catch (Exception e) {
            log.error("Error creating curriculum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error creating curriculum.");
        }
    }

    // GET /api/curriculums/{id} - Get curriculum by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCurriculumById(@PathVariable Long id) {
        try {
            CurriculumResponse response = curriculumService.getCurriculumById(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Curriculum not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching curriculum with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error fetching curriculum.");
        }
    }

    // GET /api/curriculums?careerId={careerId} - Get all curriculums (optionally filtered by career)
    @GetMapping
    public ResponseEntity<List<CurriculumResponse>> getAllCurriculums(
            @RequestParam(required = false) Long careerId // Optional filter parameter
    ) {
        try {
            List<CurriculumResponse> responseList = curriculumService.getAllCurriculums(careerId);
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error listing curriculums (careerId={}): {}", careerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // PUT /api/curriculums/{id} - Update curriculum
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCurriculum(Authentication authentication, @PathVariable Long id, @RequestBody CurriculumRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede editar mallas curriculares.");
        }
        try {
            CurriculumResponse response = curriculumService.updateCurriculum(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Failed to update non-existent curriculum ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Conflict updating curriculum ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating curriculum ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error updating curriculum.");
        }
    }

    // DELETE /api/curriculums/{id} - Delete curriculum
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCurriculum(Authentication authentication, @PathVariable Long id) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede eliminar mallas curriculares.");
        }
        try {
            curriculumService.deleteCurriculum(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            log.info("Failed to delete non-existent curriculum ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) { // Catch potential DataIntegrityViolation if courses depend on it
            log.error("Error deleting curriculum ID {}: {}", id, e.getMessage(), e);
            // Consider returning 409 Conflict if there are dependencies
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
