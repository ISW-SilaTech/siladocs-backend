package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.SyllabusResponse;
import com.siladocs.application.service.AzureBlobStorageService;
import com.siladocs.application.service.SyllabusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/public/syllabi")
@CrossOrigin(origins = {
        "https://siladocs-frontend.vercel.app",
        "http://localhost:3000"
})
public class PublicSyllabusController {

    private static final Logger log = LoggerFactory.getLogger(PublicSyllabusController.class);

    private static final int PUBLIC_SAS_EXPIRY_HOURS = 1;

    private final SyllabusService syllabusService;
    private final AzureBlobStorageService azureBlobStorageService;

    public PublicSyllabusController(SyllabusService syllabusService,
                                    AzureBlobStorageService azureBlobStorageService) {
        this.syllabusService = syllabusService;
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> getSyllabusFile(@PathVariable("id") Long id) {
        try {
            log.info("[PUBLIC] Solicitud de archivo del sílabo ID: {}", id);

            SyllabusResponse syllabus = syllabusService.getSyllabusById(id);
            String blobName = syllabus.fileUrl();

            if (blobName == null || blobName.isBlank()) {
                log.warn("[PUBLIC] El sílabo {} no tiene archivo asociado", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El sílabo no tiene archivo asociado"));
            }

            String sasUrl = azureBlobStorageService.generateDownloadSasUrl(blobName, PUBLIC_SAS_EXPIRY_HOURS);

            log.info("[PUBLIC] Redirigiendo sílabo {} a SAS URL temporal (expira en {}h)",
                    id, PUBLIC_SAS_EXPIRY_HOURS);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(sasUrl))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("[PUBLIC] Sílabo no encontrado: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sílabo no encontrado: " + id));
        } catch (Exception e) {
            log.error("[PUBLIC] Error sirviendo archivo del sílabo {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el archivo del sílabo"));
        }
    }
}
