package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.SyllabusHistoryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
@Repository
public interface SyllabusHistoryLogRepository extends JpaRepository<SyllabusHistoryLogEntity, Long> {

    /**
     * Busca todos los registros de historial para un sílabo específico,
     * ordenados por fecha descendente (el más nuevo primero).
     */
    List<SyllabusHistoryLogEntity> findBySyllabusIdOrderByChangeTimestampDesc(Long syllabusId);

}