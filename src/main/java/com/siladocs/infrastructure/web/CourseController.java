package com.siladocs.infrastructure.web;

import com.siladocs.application.dto.CourseRequest;
import com.siladocs.application.dto.CourseResponse;
import com.siladocs.application.service.CourseService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses") // Base URL for courses
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    // HU: solo Administrador Académico puede crear/editar/eliminar cursos.
    private static final String ADMIN_ROLE = "Administrador Académico";
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ADMIN_ROLE.equals(a.getAuthority()));
    }

    // POST /api/courses - Create new course
    @PostMapping
    public ResponseEntity<?> createCourse(Authentication authentication, @RequestBody CourseRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede crear cursos.");
        }
        try {
            CourseResponse response = courseService.createCourse(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            log.warn("Failed to create course: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error creating course.");
        }
    }

    // GET /api/courses/{id} - Get course by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            CourseResponse response = courseService.getCourseById(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Course not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching course with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error fetching course.");
        }
    }

    // GET /api/courses?careerId={id}&curriculumId={id} - Get all courses (optionally filtered)
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses(
            @RequestParam(required = false) Long careerId,
            @RequestParam(required = false) Long curriculumId
    ) {
        try {
            List<CourseResponse> responseList = courseService.getAllCourses(careerId, curriculumId);
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error listing courses (careerId={}, curriculumId={}): {}", careerId, curriculumId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // PUT /api/courses/{id} - Update course
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(Authentication authentication, @PathVariable Long id, @RequestBody CourseRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede editar cursos.");
        }
        try {
            CourseResponse response = courseService.updateCourse(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.info("Failed to update non-existent course ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Conflict updating course ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating course ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error updating course.");
        }
    }

    // DELETE /api/courses/{id} - Delete course
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(Authentication authentication, @PathVariable Long id) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Solo el rol '" + ADMIN_ROLE + "' puede eliminar cursos.");
        }
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            log.info("Failed to delete non-existent course ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error deleting course ID {}: {}", id, e.getMessage(), e);
            // Could be DataIntegrityViolation if Syllabuses depend on it
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
