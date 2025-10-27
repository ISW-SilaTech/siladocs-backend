package com.siladocs.domain.repository;

import com.siladocs.domain.model.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // <-- Importa @Repository

@Repository // <-- Marca como bean de repositorio
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {
    // No necesita métodos extra por ahora
}