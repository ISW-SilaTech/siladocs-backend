package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.ContactMessage;
import com.siladocs.infrastructure.persistence.entity.ContactMessageEntity;
import org.springframework.stereotype.Component;

@Component
public class ContactMessageMapper {

    public ContactMessage toDomain(ContactMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        ContactMessage domain = new ContactMessage(
            entity.getName(),
            entity.getEmail(),
            entity.getSubject(),
            entity.getMessage()
        );

        domain.setId(entity.getId());
        domain.setPhone(entity.getPhone());
        domain.setCompany(entity.getCompany());
        domain.setStatus(ContactMessage.MessageStatus.valueOf(entity.getStatus().name()));
        domain.setIpAddress(entity.getIpAddress());
        domain.setUserAgent(entity.getUserAgent());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setRepliedAt(entity.getRepliedAt());
        domain.setAdminNotes(entity.getAdminNotes());

        return domain;
    }

    public ContactMessageEntity toEntity(ContactMessage domain) {
        if (domain == null) {
            return null;
        }

        ContactMessageEntity entity = new ContactMessageEntity(
            domain.getName(),
            domain.getEmail(),
            domain.getSubject(),
            domain.getMessage()
        );

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        entity.setPhone(domain.getPhone());
        entity.setCompany(domain.getCompany());
        entity.setStatus(ContactMessageEntity.MessageStatus.valueOf(domain.getStatus().name()));
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setRepliedAt(domain.getRepliedAt());
        entity.setAdminNotes(domain.getAdminNotes());

        return entity;
    }
}
