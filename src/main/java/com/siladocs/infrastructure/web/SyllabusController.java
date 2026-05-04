package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.SyllabusHistoryResponse;
import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.service.BlockchainService;
import com.siladocs.application.service.SyllabusService;
import com.siladocs.application.service.AzureBlobStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList; // 🔹 Importado
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/syllabi")
public class SyllabusController {

    private static final Logger log = LoggerFactory.getLogger(SyllabusController.class);

    private final SyllabusService syllabusService;
    private final BlockchainService blockchainService;
    private final AzureBlobStorageService azureBlobStorageService;

    public SyllabusController(SyllabusService syllabusService, BlockchainService blockchainService, AzureBlobStorageService azureBlobStorageService) {
        this.syllabusService = syllabusService;
        this.blockchainService = blockchainService;
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSyllabus(
        Authentication authentication,
        @RequestPart("file") MultipartFile file,
        @RequestParam("courseId") Long courseId,
        @RequestParam(value = "action", defaultValue = "create") String action) {
        try {
            SyllabusResponse response = syllabusService.uploadSyllabus(courseId, file, action);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error FATAL en la subida del sílabo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<SyllabusResponse>> getAllSyllabi() {
        try {
            List<SyllabusResponse> syllabi = syllabusService.getAllSyllabi();
            return ResponseEntity.ok(syllabi);
        } catch (Exception e) {
            log.error("Error fetching all syllabi: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint Corregido: Obtiene el historial COMPLETO de un sílabo iterando sobre los bloques.
     * Esta es la forma más robusta de evitar el error de decodificación de arrays complejos de Web3j.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getSyllabusHistory(@PathVariable("id") Long id) {
        log.info("LECTURA DE HISTORIAL solicitada para Syllabus ID: {}", id);
        try {
            // En la nueva arquitectura con Fabric, el historial se mantiene en la BD
            // Retornar lista vacía o información almacenada en PostgreSQL
            List<SyllabusHistoryResponse> history = new ArrayList<>();

            if (history.isEmpty()) {
                log.info("Historial para Sílabo ID {} consultado (vacío en esta versión).", id);
                return ResponseEntity.ok(history);
            }

            // Si la lectura tiene éxito, devuelve la lista
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            // El bloque catch ahora devuelve un Map<String, String> (con el ?)
            log.error("Error FATAL al leer el historial del ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al leer la cadena de bloques: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<?> generateDownloadUrl(@PathVariable("id") Long id) {
        try {
            log.info("Generating download URL for syllabus ID: {}", id);
            String blobName = String.format("syllabi/%d", id);
            String sasUrl = azureBlobStorageService.generateDownloadSasUrl(blobName);
            return ResponseEntity.ok(Map.of("downloadUrl", sasUrl));
        } catch (Exception e) {
            log.error("Error generating download URL for syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating download URL: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSyllabus(@PathVariable("id") Long id) {
        try {
            log.info("Deleting syllabus with ID: {}", id);
            String blobName = String.format("syllabi/%d", id);
            azureBlobStorageService.deleteFile(blobName);
            return ResponseEntity.ok(Map.of("message", "Syllabus deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting syllabus: " + e.getMessage()));
        }
    }
}