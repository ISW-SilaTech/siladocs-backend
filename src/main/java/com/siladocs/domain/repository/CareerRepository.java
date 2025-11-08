package com.siladocs.domain.repository;

import com.siladocs.domain.model.Career;
import java.util.List;
import java.util.Optional;

public interface CareerRepository {
    Career save(Career career);
    Optional<Career> findById(Long id);
    List<Career> findAll();
    void deleteById(Long id);
    boolean existsByName(String name);
}