package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.SyllabusVersionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusVersionHistoryJpaRepository extends JpaRepository<SyllabusVersionHistoryEntity, Long> {

    List<SyllabusVersionHistoryEntity> findBySyllabus_IdOrderByVersionNumberDesc(Long syllabusId);

    Optional<SyllabusVersionHistoryEntity> findBySyllabus_IdAndVersionNumber(Long syllabusId, Integer versionNumber);
}
