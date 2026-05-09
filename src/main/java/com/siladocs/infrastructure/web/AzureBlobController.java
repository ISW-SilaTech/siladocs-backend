package com.siladocs.infrastructure.web;

import com.siladocs.application.service.AzureBlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/azure-blob")
public class AzureBlobController {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobController.class);

    private final AzureBlobStorageService azureBlobStorageService;

    public AzureBlobController(AzureBlobStorageService azureBlobStorageService) {
        this.azureBlobStorageService = azureBlobStorageService;
    }

    @GetMapping("/preview-url")
    public ResponseEntity<?> getPreviewUrl(@RequestParam("fileName") String fileName) {
        try {
            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "fileName parameter is required"));
            }

            log.info("Generating preview URL for file: {}", fileName);
            String sasUrl = azureBlobStorageService.generateDownloadSasUrl(fileName);

            return ResponseEntity.ok(Map.of("previewUrl", sasUrl));
        } catch (Exception e) {
            log.error("Error generating preview URL for file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating preview URL: " + e.getMessage()));
        }
    }

    @GetMapping("/download-url")
    public ResponseEntity<?> getDownloadUrl(@RequestParam("fileName") String fileName) {
        try {
            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "fileName parameter is required"));
            }

            log.info("Generating download URL for file: {}", fileName);
            String sasUrl = azureBlobStorageService.generateDownloadSasUrl(fileName);

            return ResponseEntity.ok(Map.of("downloadUrl", sasUrl));
        } catch (Exception e) {
            log.error("Error generating download URL for file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating download URL: " + e.getMessage()));
        }
    }
}
