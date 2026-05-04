package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.ContactRequestEntity; // 🔹 1. Apunta a la ENTIDAD
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
@Repository
// 🔹 2. El repositorio debe gestionar la ENTIDAD, no el modelo de dominio
public interface ContactRequestRepository extends JpaRepository<ContactRequestEntity, Long> {
    // No necesita métodos extra por ahora
}