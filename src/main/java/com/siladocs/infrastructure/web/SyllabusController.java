package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.SyllabusHistoryResponse;
import com.siladocs.application.service.BlockchainService;
import com.siladocs.application.service.SyllabusService;
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
    private final BlockchainService blockchainService;

    public SyllabusController(SyllabusService syllabusService, BlockchainService blockchainService) {
        this.syllabusService = syllabusService;
        this.blockchainService = blockchainService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSyllabus(Authentication authentication, // ⬅️ Dejamos esto para la seguridad
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("courseId") Long courseId,
                                            @RequestParam("action") String action) {
        try {
            // 🔹 (Opcional) Validar que el usuario esté autenticado
            // if (authentication == null || !authentication.isAuthenticated()) {
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
            // }
            // String userEmail = authentication.getName(); // ⬅️ Ya no pasamos esto

            // 1. Simulación de subida a Minio
            String fileUrl = "http://minio:9000/siladocs/" + file.getOriginalFilename();
            String fileContent = new String(file.getBytes()); // Simulación (para hashing)

            // 2. ⬇️ 🔹 --- CORRECCIÓN AQUÍ --- 🔹 ⬇️
            //    Llamar al servicio sin el 'userEmail'
            syllabusService.uploadSyllabus(courseId, fileContent, fileUrl, action);
            // ⬆️ 🔹 --- FIN DE LA CORRECCIÓN --- 🔹 ⬆️

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Sílabo registrado exitosamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getSyllabusHistory(@PathVariable Long id) {
        try {
            // Llama al BlockchainService para leer la cadena
            List<SyllabusHistoryResponse> history = blockchainService.getSyllabusHistory(id);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}