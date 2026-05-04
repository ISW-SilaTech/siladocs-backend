package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.domain.model.Institution;
import com.siladocs.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
@Repository
public interface InstitutionJpaRepository extends JpaRepository<InstitutionEntity, Long> {

    boolean existsByDomain(String domain);
    
    // ⬇️ CORRECCIÓN: Debe devolver InstitutionEntity, NO Institution
    InstitutionEntity findByName(String name); 
    
    Optional<InstitutionEntity> findByDomain(String domain);
}
