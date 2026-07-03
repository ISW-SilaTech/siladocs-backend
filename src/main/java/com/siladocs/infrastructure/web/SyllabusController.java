package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.SyllabusHistoryResponse;
import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.dto.SyllabusVersionDto;
import com.siladocs.application.service.BlockchainService;
import com.siladocs.application.service.SyllabusService;
import com.siladocs.application.service.SyllabusVersionService;
import com.siladocs.application.service.AzureBlobStorageService;
import com.siladocs.application.service.FileAnalysisService;
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
import java.util.Set;

@RestController
@RequestMapping("/syllabi")
public class SyllabusController {

    private static final Logger log = LoggerFactory.getLogger(SyllabusController.class);

    private final SyllabusService syllabusService;
    private final BlockchainService blockchainService;
    private final AzureBlobStorageService azureBlobStorageService;
    private final SyllabusVersionService versionService;
    private final FileAnalysisService fileAnalysisService;

    public SyllabusController(SyllabusService syllabusService, BlockchainService blockchainService,
                            AzureBlobStorageService azureBlobStorageService, SyllabusVersionService versionService,
                            FileAnalysisService fileAnalysisService) {
        this.syllabusService = syllabusService;
        this.blockchainService = blockchainService;
        this.azureBlobStorageService = azureBlobStorageService;
        this.versionService = versionService;
        this.fileAnalysisService = fileAnalysisService;
    }

    // HU: solo Administrador Académico y Docente pueden subir sílabos.
    private static final Set<String> UPLOAD_SYLLABUS_ROLES = Set.of("Administrador Académico", "Docente");

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSyllabus(
        Authentication authentication,
        @RequestPart("file") MultipartFile file,
        @RequestParam("courseId") Long courseId,
        @RequestParam(value = "action", defaultValue = "create") String action,
        @RequestParam(value = "sessionId", required = false) String sessionId) {
        boolean isAuthorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> UPLOAD_SYLLABUS_ROLES.contains(a.getAuthority()));
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo los roles 'Administrador Académico' o 'Docente' pueden subir sílabos."));
        }
        try {
            SyllabusResponse response = syllabusService.uploadSyllabus(courseId, file, action, sessionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error FATAL en la subida del sílabo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/analyze", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeSyllabusFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("courseCode") String courseCode) {
        try {
            log.info("[FILE ANALYSIS API] Analyzing file: {} for course: {}", file.getOriginalFilename(), courseCode);
            FileAnalysisService.FileAnalysisResult result = fileAnalysisService.analyzeFile(file, courseCode);

            return ResponseEntity.ok(Map.of(
                    "courseCode", result.courseCode,
                    "detectedCode", result.detectedCode,
                    "confidence", result.confidence,
                    "allDetectedCodes", result.allDetectedCodes,
                    "isMatch", result.isMatch,
                    "message", buildAnalysisMessage(result)
            ));
        } catch (Exception e) {
            log.error("[FILE ANALYSIS API] Error analyzing file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al analizar el archivo: " + e.getMessage()));
        }
    }

    private String buildAnalysisMessage(FileAnalysisService.FileAnalysisResult result) {
        if (!result.isMatch) {
            if (result.detectedCode == null) {
                return "No se detectó código de curso en el archivo. Verifica que el nombre o contenido contenga el código del curso.";
            }
            return String.format("El código detectado (%s) no coincide exactamente con el curso (%s). Confianza: %.0f%%",
                    result.detectedCode, result.courseCode, result.confidence * 100);
        }
        return String.format("✓ Código de curso detectado correctamente: %s (Confianza: %.0f%%)",
                result.detectedCode, result.confidence * 100);
    }

    @GetMapping
    public ResponseEntity<List<SyllabusResponse>> getAllSyllabi() {
        try {
            log.info("[SYLLABI API] GET /syllabi endpoint called");
            List<SyllabusResponse> syllabi = syllabusService.getAllSyllabi();
            log.info("[SYLLABI API] Returning {} syllabi", syllabi.size());
            return ResponseEntity.ok(syllabi);
        } catch (Exception e) {
            log.error("Error fetching all syllabi: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/debug/count")
    public ResponseEntity<?> getDebugInfo() {
        try {
            // Debug endpoint para investigar el problema de 4 sílabos
            log.info("[DEBUG] Debug endpoint called");

            // Total incluyendo eliminados
            long totalWithDeleted = syllabusService.getAllSyllabiForAudit().size();
            log.info("[DEBUG] Total syllabi (including deleted): {}", totalWithDeleted);

            // Total sin eliminar
            List<SyllabusResponse> activeOnly = syllabusService.getAllSyllabi();
            log.info("[DEBUG] Active syllabi (deleted=false): {}", activeOnly.size());

            return ResponseEntity.ok(Map.of(
                    "totalWithDeleted", totalWithDeleted,
                    "activeOnly", activeOnly.size(),
                    "syllabi", activeOnly
            ));
        } catch (Exception e) {
            log.error("[DEBUG] Error in debug endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit/all")
    public ResponseEntity<?> getAllSyllabiForAudit(Authentication authentication) {
        // HU0010: Endpoint de auditoría. Retorna TODOS los sílabos incluyendo eliminados.
        // Restringido a administradores para verificar integridad blockchain de documentos
        // que ya fueron eliminados lógicamente.
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Autenticación requerida"));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "Administrador Académico".equals(a.getAuthority()));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo administradores pueden acceder al historial completo de auditoría"));
        }

        try {
            List<SyllabusResponse> syllabi = syllabusService.getAllSyllabiForAudit();
            return ResponseEntity.ok(syllabi);
        } catch (Exception e) {
            log.error("Error fetching all syllabi for audit: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint Corregido: Obtiene el historial COMPLETO de un sílabo iterando sobre los bloques.
     * Esta es la forma más robusta de evitar el error de decodificación de arrays complejos de Web3j.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSyllabusById(@PathVariable("id") Long id) {
        try {
            SyllabusResponse syllabus = syllabusService.getSyllabusById(id);
            return ResponseEntity.ok(syllabus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

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
            com.siladocs.application.dto.SyllabusResponse syllabus = syllabusService.getSyllabusById(id);
            String blobName = syllabus.fileUrl();
            if (blobName == null || blobName.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No file URL stored for syllabus " + id));
            }
            String sasUrl = azureBlobStorageService.generateDownloadSasUrl(blobName);
            return ResponseEntity.ok(Map.of("downloadUrl", sasUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating download URL for syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating download URL: " + e.getMessage()));
        }
    }

    // HU: solo Administrador Académico puede aprobar (validar) un sílabo.
    private static final String APPROVE_SYLLABUS_ROLE = "Administrador Académico";

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveSyllabus(@PathVariable("id") Long id, Authentication authentication) {
        boolean isAuthorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> APPROVE_SYLLABUS_ROLE.equals(a.getAuthority()));
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo el rol '" + APPROVE_SYLLABUS_ROLE + "' puede aprobar sílabos."));
        }
        try {
            String approverEmail = authentication != null ? authentication.getName() : "system";
            log.info("Aprobando sílabo ID {} por {}", id, approverEmail);
            SyllabusResponse response = syllabusService.approveSyllabus(id, approverEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error aprobando sílabo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al aprobar sílabo: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/verify-integrity")
    public ResponseEntity<?> verifyIntegrity(@PathVariable("id") Long id) {
        try {
            log.info("Verificando integridad de sílabo ID {}", id);
            Map<String, Object> result = syllabusService.verifyIntegrity(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error verificando integridad del sílabo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar integridad: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifySyllabusImmutability(@PathVariable("id") Long id) {
        try {
            log.info("Verificando inmutabilidad del sílabo ID {}", id);
            SyllabusResponse syllabus = syllabusService.getSyllabusById(id);

            // Un sílabo se considera verificado (inmutable) si tiene un fabricTxId válido
            boolean verified = syllabus.fabricTxId() != null && !syllabus.fabricTxId().isEmpty();

            // El número de bloque indica en qué bloque de la blockchain se registró
            // Por ahora usamos 1 como valor por defecto para sílabos verificados
            int blockNumber = verified ? 1 : 0;

            log.info("Inmutabilidad verificada para sílabo {}: verified={}, block={}, txId={}",
                    id, verified, blockNumber, syllabus.fabricTxId());

            return ResponseEntity.ok(Map.of(
                    "verified", verified,
                    "block", blockNumber
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Sílabo no encontrado: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error verificando inmutabilidad del sílabo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error verificando inmutabilidad: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getSyllabusVersionHistory(@PathVariable("id") Long id) {
        try {
            log.info("Obteniendo historial de versiones del sílabo ID {}", id);
            List<SyllabusVersionDto> versions = versionService.getSyllabusVersionHistory(id);
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            log.error("Error obteniendo historial de versiones del sílabo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error obteniendo historial de versiones: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<?> getSpecificVersion(@PathVariable("id") Long id, @PathVariable("versionNumber") Integer versionNumber) {
        try {
            log.info("Obteniendo versión {} del sílabo ID {}", versionNumber, id);
            SyllabusVersionDto version = versionService.getSpecificVersion(id, versionNumber);
            return ResponseEntity.ok(version);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo versión {} del sílabo {}: {}", versionNumber, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error obteniendo versión: " + e.getMessage()));
        }
    }

    // HU0010: eliminación lógica restringida al rol "Administrador Académico".
    // El archivo, el hash y el historial de versiones se conservan para
    // mantener la trazabilidad/auditoría en blockchain; solo se oculta del
    // listado activo de sílabos.
    private static final String DELETE_SYLLABUS_ROLE = "Administrador Académico";

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSyllabus(@PathVariable("id") Long id, Authentication authentication) {
        boolean isAuthorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> DELETE_SYLLABUS_ROLE.equals(a.getAuthority()));

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo el rol '" + DELETE_SYLLABUS_ROLE + "' puede eliminar sílabos."));
        }

        try {
            log.info("Deleting (soft) syllabus with ID: {} by {}", id, authentication.getName());
            syllabusService.softDeleteSyllabus(id, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Sílabo eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el sílabo: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/register-version")
    public ResponseEntity<?> registerVersionRetroactively(
            @PathVariable("id") Long id,
            @RequestParam(value = "versionNumber", defaultValue = "1") Integer versionNumber,
            @RequestParam(value = "fileUrl", required = true) String fileUrl,
            @RequestParam(value = "fileHash", required = true) String fileHash,
            @RequestParam(value = "uploadedBy", defaultValue = "Sistema") String uploadedBy,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "fabricTxId", required = false) String fabricTxId) {
        try {
            log.info("Registering version {} for syllabus {}", versionNumber, id);

            SyllabusResponse syllabus = syllabusService.getSyllabusById(id);
            com.siladocs.infrastructure.persistence.entity.SyllabusEntity entity =
                    new com.siladocs.infrastructure.persistence.entity.SyllabusEntity();
            entity.setId(id);

            versionService.recordVersion(
                    entity,
                    versionNumber,
                    "registered",
                    uploadedBy,
                    notes != null ? notes : "Versión registrada retroactivamente",
                    fileUrl,
                    fileHash,
                    fabricTxId
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Versión registrada exitosamente",
                    "syllabusId", id,
                    "versionNumber", versionNumber
            ));
        } catch (Exception e) {
            log.error("Error registering version for syllabus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error registering version: " + e.getMessage()));
        }
    }
}
