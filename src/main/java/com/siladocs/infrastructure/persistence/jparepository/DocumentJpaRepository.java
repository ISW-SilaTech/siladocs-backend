package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

    // Buscar documento por hash
    Optional<DocumentEntity> findByHash(String hash);

    // Verificar si existe un documento con un hash específico
    boolean existsByHash(String hash);
}
