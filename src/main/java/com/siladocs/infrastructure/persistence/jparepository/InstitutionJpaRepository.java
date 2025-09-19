package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionJpaRepository extends JpaRepository<InstitutionEntity, Long> {

    // Buscar institución por dominio (opcional, útil para validaciones)
    Optional<InstitutionEntity> findByDomain(String domain);

    // Verificar si existe institución con un dominio
    boolean existsByDomain(String domain);
}
