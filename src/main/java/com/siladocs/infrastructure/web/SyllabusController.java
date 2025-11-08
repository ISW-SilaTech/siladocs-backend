package com.siladocs.infrastructure.web; // O com.siladocs.application.controller

import com.siladocs.application.service.SyllabusService;
import com.siladocs.infrastructure.persistence.entity.SyllabusHistoryLogEntity; // O un DTO
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/syllabi")
public class SyllabusController {

    private final SyllabusService syllabusService;
    // (Aquí también necesitarás tu servicio de Minio/Storage)

    public SyllabusController(SyllabusService syllabusService) {
        this.syllabusService = syllabusService;
    }

    /**
     * Endpoint para subir un nuevo sílabo (o versión)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSyllabus(Authentication authentication,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("courseId") Long courseId,
                                            @RequestParam("action") String action) { // Ej: "CARGA_INICIAL"
        try {
            String userEmail = authentication.getName();

            // 1. Subir el archivo a Minio (necesitarás tu servicio de storage aquí)
            // String fileUrl = storageService.uploadFile(file);
            String fileUrl = "http://minio:9000/siladocs/" + file.getOriginalFilename(); // Simulación
            String fileContent = new String(file.getBytes()); // Simulación (para hashing)

            // 2. Llamar al servicio de sílabo (que llama a la blockchain)
            syllabusService.uploadSyllabus(courseId, userEmail, fileContent, fileUrl, action);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Sílabo registrado exitosamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para obtener el historial de trazabilidad (la "Blockchain")
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getSyllabusHistory(@PathVariable Long id) {
        try {
            // Esto llama al método que lee de la tabla de historial (nuestra blockchain-lite)
            // o que llama a `blockchainService.getHistory` (si implementamos la lectura de Ganache)
            // List<SyllabusHistoryLogEntity> history = syllabusService.getSyllabusHistory(id);
            // return ResponseEntity.ok(history);

            // Placeholder hasta que implementes la lectura
            return ResponseEntity.ok(Map.of("message", "Historial para sílabo " + id));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}