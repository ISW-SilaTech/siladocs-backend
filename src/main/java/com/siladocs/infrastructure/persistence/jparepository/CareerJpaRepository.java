package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(exported = false)
@Repository
public interface CareerJpaRepository extends JpaRepository<CareerEntity, Long> {

    boolean existsByName(String name);

    Optional<CareerEntity> findByName(String name);

    Optional<CareerEntity> findByNameIgnoreCase(String name);
}
