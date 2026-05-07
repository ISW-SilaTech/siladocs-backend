package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.SyllabusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusJpaRepository extends JpaRepository<SyllabusEntity, Long> {

    // (Opcional, pero útil) Buscar todos los sílabos de un curso
    List<SyllabusEntity> findByCourse_IdOrderByCurrentVersionDesc(Long courseId);

    // (Opcional, pero útil) Buscar la última versión de un sílabo por ID de curso
    Optional<SyllabusEntity> findFirstByCourse_IdOrderByCurrentVersionDesc(Long courseId);
}