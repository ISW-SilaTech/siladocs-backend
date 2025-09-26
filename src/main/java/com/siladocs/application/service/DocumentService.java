package com.siladocs.application.service;

import com.siladocs.application.dto.DocumentResponse;
import com.siladocs.domain.model.Document;
import com.siladocs.infrastructure.persistence.entity.DocumentEntity;
import com.siladocs.infrastructure.persistence.jparepository.DocumentJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentJpaRepository documentRepo;
    private final DocumentMapper mapper;

    // ---------- Subir un nuevo documento ----------
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) throws Exception {
        String hash = calculateHash(file.getBytes());

        if (documentRepo.existsByHash(hash)) {
            throw new RuntimeException("El documento ya existe con ese hash");
        }

        Document domain = new Document(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                hash,
                Instant.now()
        );

        DocumentEntity entity = mapper.toEntity(domain);
        DocumentEntity saved = documentRepo.save(entity);

        return toResponse(saved);
    }

    // ---------- Actualizar documento (ej. metadata) ----------
    @Transactional
    public DocumentResponse updateDocument(Long id, String newFileName) {
        DocumentEntity entity = documentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        entity.setFileName(newFileName); // solo actualizamos nombre por ahora
        DocumentEntity updated = documentRepo.save(entity);

        return toResponse(updated);
    }

    // ---------- Obtener documento por ID ----------
    public DocumentResponse getDocument(Long id) {
        DocumentEntity entity = documentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));
        return toResponse(entity);
    }

    // ---------- Listar todos los documentos ----------
    public List<DocumentResponse> listDocuments() {
        return documentRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---------- Eliminar documento ----------
    @Transactional
    public void deleteDocument(Long id) {
        if (!documentRepo.existsById(id)) {
            throw new RuntimeException("Documento no encontrado");
        }
        documentRepo.deleteById(id);
    }

    // ---------- Helper: convertir Entity → Response ----------
    private DocumentResponse toResponse(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getFileName(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getHash(),
                entity.getUploadedAt()
        );
    }

    // ---------- Helper: calcular hash ----------
    private String calculateHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
