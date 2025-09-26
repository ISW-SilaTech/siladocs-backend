package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.Document;
import com.siladocs.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    // Convierte de Entity a Domain
    public Document toDomain(DocumentEntity entity) {
        if (entity == null) return null;
        return new Document(
                entity.getFileName(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getHash(),
                entity.getUploadedAt()
        );
    }

    // Convierte de Domain a Entity
    public DocumentEntity toEntity(Document domain) {
        if (domain == null) return null;
        return new DocumentEntity(
                domain.getFileName(),
                domain.getHash(),
                domain.getFileType(),
                domain.getFileSize()
        );
    }

    // Actualiza una entidad existente con datos del dominio
    public void updateEntity(DocumentEntity entity, Document domain) {
        entity.setFileName(domain.getFileName());
        entity.setFileType(domain.getFileType());
        entity.setFileSize(domain.getFileSize());
        entity.setHash(domain.getHash());
    }
}
