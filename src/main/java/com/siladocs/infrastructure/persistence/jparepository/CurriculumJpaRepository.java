package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumJpaRepository extends JpaRepository<CurriculumEntity, Long> {

    List<CurriculumEntity> findByCareerId(Long careerId);

    boolean existsByNameAndCareerId(String name, Long careerId);

    Optional<CurriculumEntity> findByNameIgnoreCaseAndCareerId(String name, Long careerId);
}
