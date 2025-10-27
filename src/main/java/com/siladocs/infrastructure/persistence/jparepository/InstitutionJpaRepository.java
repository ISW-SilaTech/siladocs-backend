package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionJpaRepository extends JpaRepository<InstitutionEntity, Long> {

    /**
     * Método nuevo: Spring Data JPA lee el nombre del método
     * y automáticamente genera una consulta "EXISTS"
     * (ej: "SELECT 1 FROM institutions WHERE domain = ?").
     * Esto es más eficiente que traer el objeto completo.
     */
    boolean existsByDomain(String domain);

    // (Este método de abajo es el que usaba tu RegistrationService,
    // es bueno tener ambos)
    Optional<InstitutionEntity> findByDomain(String domain);
}