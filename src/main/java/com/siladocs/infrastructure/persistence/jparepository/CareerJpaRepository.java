package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerJpaRepository extends JpaRepository<CareerEntity, Long> {

    boolean existsByName(String name);

    Optional<CareerEntity> findByName(String name);

    Optional<CareerEntity> findByNameIgnoreCase(String name);
}
