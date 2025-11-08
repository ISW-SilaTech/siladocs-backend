package com.siladocs.domain.repository;

import com.siladocs.domain.model.Syllabus;
import java.util.Optional;

public interface SyllabusRepository {
    Syllabus save(Syllabus syllabus);
    Optional<Syllabus> findById(Long syllabusId);
}