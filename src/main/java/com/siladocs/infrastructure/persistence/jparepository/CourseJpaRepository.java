package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    // Find courses by curriculum ID
    List<CourseEntity> findByCurriculumId(Long curriculumId);

    // Find courses by career ID
    List<CourseEntity> findByCareerId(Long careerId);

    // Check if a course code already exists
    boolean existsByCode(String code);

    // Check if code exists within a specific curriculum (more specific validation)
    boolean existsByCodeAndCurriculumId(String code, Long curriculumId);
}