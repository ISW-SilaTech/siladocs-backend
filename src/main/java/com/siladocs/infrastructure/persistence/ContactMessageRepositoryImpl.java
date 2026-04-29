package com.siladocs.infrastructure.persistence;

import com.siladocs.domain.model.ContactMessage;
import com.siladocs.domain.repository.ContactMessageRepository;
import com.siladocs.infrastructure.persistence.entity.ContactMessageEntity;
import com.siladocs.infrastructure.persistence.jparepository.ContactMessageJpaRepository;
import com.siladocs.infrastructure.persistence.mapper.ContactMessageMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ContactMessageRepositoryImpl implements ContactMessageRepository {

    private final ContactMessageJpaRepository jpaRepository;
    private final ContactMessageMapper mapper;

    public ContactMessageRepositoryImpl(ContactMessageJpaRepository jpaRepository, ContactMessageMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ContactMessage save(ContactMessage message) {
        ContactMessageEntity entity = mapper.toEntity(message);
        ContactMessageEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ContactMessage> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ContactMessage> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ContactMessage> findByStatus(String status) {
        try {
            ContactMessageEntity.MessageStatus messageStatus = ContactMessageEntity.MessageStatus.valueOf(status);
            return jpaRepository.findByStatus(messageStatus)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public List<ContactMessage> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID id, String status) {
        Optional<ContactMessageEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isPresent()) {
            try {
                ContactMessageEntity entity = entityOptional.get();
                entity.setStatus(ContactMessageEntity.MessageStatus.valueOf(status));
                jpaRepository.save(entity);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status, e);
            }
        }
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
