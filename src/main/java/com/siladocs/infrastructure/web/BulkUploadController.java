package com.siladocs.infrastructure.web;

import com.siladocs.application.service.BulkUploadService;
import com.siladocs.infrastructure.web.dto.BulkCourseRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulk-upload") // Base URL for bulk operations
public class BulkUploadController {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadController.class);
    private final BulkUploadService bulkUploadService;

    public BulkUploadController(BulkUploadService bulkUploadService) {
        this.bulkUploadService = bulkUploadService;
    }

    @PostMapping("/courses")
    public ResponseEntity<?> uploadCourses(@RequestBody List<BulkCourseRequestDto> requests) {
        log.info("Recibida solicitud de carga masiva con {} registros.", requests.size());
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body("La lista de registros no puede estar vacía.");
        }

        try {
            BulkUploadService.BulkUploadResult result = bulkUploadService.processBulkCourses(requests);

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
                    .body("Error interno del servidor durante la carga masiva.");
        }
    }
}