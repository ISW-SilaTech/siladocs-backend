package com.siladocs.domain.repository;

import com.siladocs.domain.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(Long id);
    List<Course> findAllBy(Long careerId, Long curriculumId);
    void deleteById(Long id);
    boolean existsByCode(String code);
}