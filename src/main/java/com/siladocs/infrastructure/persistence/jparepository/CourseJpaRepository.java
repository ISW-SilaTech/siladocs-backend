package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findByCurriculumId(Long curriculumId);

    List<CourseEntity> findByCareerId(Long careerId);

    boolean existsByCode(String code);

    boolean existsByCodeAndCurriculumId(String code, Long curriculumId);

    Optional<CourseEntity> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndCurriculumId(String name, Long curriculumId);
}
