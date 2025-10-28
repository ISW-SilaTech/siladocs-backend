package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CurriculumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CurriculumJpaRepository extends JpaRepository<CurriculumEntity, Long> {

    // Find all curriculums belonging to a specific career
    List<CurriculumEntity> findByCareerId(Long careerId);

    // Check if a curriculum with this name exists for a specific career (useful for validation)
    boolean existsByNameAndCareerId(String name, Long careerId);
}