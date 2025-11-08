package com.siladocs.infrastructure.web;

import com.siladocs.application.service.BulkUploadService;
import com.siladocs.infrastructure.web.dto.BulkCourseRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // 🔹 Importar
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // 🔹 Importar

@RestController
@RequestMapping("/api/bulk-upload") // Base URL for bulk operations
public class BulkUploadController {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadController.class);
    private final BulkUploadService bulkUploadService;

    public BulkUploadController(BulkUploadService bulkUploadService) {
        this.bulkUploadService = bulkUploadService;
    }

    @PostMapping("/courses")
    // 🔹 1. Añadir 'Authentication authentication' para obtener el usuario del token
    public ResponseEntity<?> uploadCourses(@RequestBody List<BulkCourseRequestDto> requests, Authentication authentication) {

        // 🔹 2. Validar que el usuario esté autenticado
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }
        String userEmail = authentication.getName(); // Obtener el email del token

        log.info("Recibida solicitud de carga masiva de {} registros por usuario: {}", requests.size(), userEmail);
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La lista de registros no puede estar vacía."));
        }

        try {
            // 🔹 3. Pasar el 'userEmail' al servicio
            BulkUploadService.BulkUploadResult result = bulkUploadService.processBulkCourses(requests, userEmail);

            if (!result.errors().isEmpty()) {
                // Return 207 Multi-Status if there were partial failures
                log.warn("Carga masiva completada con {} errores.", result.errors().size());
                return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
            } else {
                // Return 201 Created if all succeeded
                log.info("Carga masiva completada exitosamente ({} registros).", result.successCount());
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            }
        } catch (Exception e) {
            log.error("Error crítico durante la carga masiva: {}", e.getMessage(), e);
            // Return 500 for unexpected server errors during processing
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor durante la carga masiva."));
        }
    }
}