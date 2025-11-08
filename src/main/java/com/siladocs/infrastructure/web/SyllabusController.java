package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.SyllabusHistoryResponse;
import com.siladocs.application.service.BlockchainService;
import com.siladocs.application.service.SyllabusService;
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
@RequestMapping("/api/syllabi")
public class SyllabusController {

    private static final Logger log = LoggerFactory.getLogger(SyllabusController.class);

    private final SyllabusService syllabusService;
    private final BlockchainService blockchainService;

    public SyllabusController(SyllabusService syllabusService, BlockchainService blockchainService) {
        this.syllabusService = syllabusService;
        this.blockchainService = blockchainService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSyllabus(Authentication authentication,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("courseId") Long courseId,
                                            @RequestParam("action") String action) {
        try {
            // 1. Simulación de subida a Minio
            String fileUrl = "http://minio:9000/siladocs/" + file.getOriginalFilename();
            String fileContent = new String(file.getBytes());

            // 2. Llamar al servicio sin el 'userEmail' (el servicio lo obtiene internamente)
            syllabusService.uploadSyllabus(courseId, fileContent, fileUrl, action);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Sílabo registrado exitosamente"));

        } catch (Exception e) {
            log.error("Error FATAL en la subida del sílabo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint Corregido: Obtiene el historial COMPLETO de un sílabo iterando sobre los bloques.
     * Esta es la forma más robusta de evitar el error de decodificación de arrays complejos de Web3j.
     */
    @GetMapping("/{id}/history")
    // 🔹 CORRECCIÓN CLAVE: Cambia el tipo de retorno a ResponseEntity<?>
    public ResponseEntity<?> getSyllabusHistory(@PathVariable("id") Long id) {
        log.info("LECTURA DE BLOCKCHAIN solicitada para Syllabus ID: {}", id);
        try {
            // Llama al BlockchainService para leer la cadena
            List<SyllabusHistoryResponse> history = blockchainService.getSyllabusHistory(id);

            // Si la lista está vacía, devuelve un mensaje más informativo
            if (history.isEmpty()) {
                log.warn("El historial para el Sílabo ID {} fue leído pero está vacío.", id);
                // Retorna la lista vacía (tipo List<SyllabusHistoryResponse>)
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
}