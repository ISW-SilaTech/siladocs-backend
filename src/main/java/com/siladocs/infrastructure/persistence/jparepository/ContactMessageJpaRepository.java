package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.ContactMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RepositoryRestResource(exported = false)
public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageEntity, UUID> {

    List<ContactMessageEntity> findByStatus(ContactMessageEntity.MessageStatus status);

    List<ContactMessageEntity> findByEmail(String email);

    Page<ContactMessageEntity> findByStatus(ContactMessageEntity.MessageStatus status, Pageable pageable);

    Page<ContactMessageEntity> findAll(Pageable pageable);
}
