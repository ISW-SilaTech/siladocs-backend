package com.siladocs.domain.repository;

import com.siladocs.domain.model.ContactMessage;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ContactMessageRepository {

    ContactMessage save(ContactMessage message);

    Optional<ContactMessage> findById(UUID id);

    List<ContactMessage> findAll();

    List<ContactMessage> findByStatus(String status);

    List<ContactMessage> findByEmail(String email);

    void updateStatus(UUID id, String status);

    void delete(UUID id);
}
