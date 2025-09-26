package com.siladocs.application.controller;

import com.siladocs.application.dto.DocumentResponse;
import com.siladocs.application.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Endpoints para gestión de documentos")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ---------- Subir documento ----------
    @PostMapping("/upload")
    @Operation(summary = "Subir documento", description = "Carga un nuevo documento al sistema")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            DocumentResponse response = documentService.uploadDocument(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------- Obtener documento por ID ----------
    @GetMapping("/{id}")
    @Operation(summary = "Obtener documento", description = "Devuelve un documento por ID")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        try {
            DocumentResponse response = documentService.getDocument(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ---------- Listar documentos ----------
    @GetMapping
    @Operation(summary = "Listar documentos", description = "Devuelve todos los documentos almacenados")
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        List<DocumentResponse> documents = documentService.listDocuments();
        return ResponseEntity.ok(documents);
    }

    // ---------- Actualizar documento ----------
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar documento", description = "Actualiza el nombre del documento")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestParam String newFileName
    ) {
        try {
            DocumentResponse response = documentService.updateDocument(id, newFileName);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ---------- Eliminar documento ----------
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar documento", description = "Elimina un documento por ID")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
