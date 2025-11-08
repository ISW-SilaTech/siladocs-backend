package com.siladocs.domain.repository;

import com.siladocs.domain.model.Curriculum;
import java.util.List;
import java.util.Optional;

public interface CurriculumRepository {
    Curriculum save(Curriculum curriculum);
    Optional<Curriculum> findById(Long id);
    List<Curriculum> findByCareerId(Long careerId);
    void deleteById(Long id);
    boolean existsByNameAndCareerId(String name, Long careerId);
}