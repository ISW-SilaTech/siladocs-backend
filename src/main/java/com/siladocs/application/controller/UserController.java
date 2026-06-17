package com.siladocs.application.controller;

import com.siladocs.domain.model.Institution;
import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

record UpdateProfileRequest(String fullName, String language, String timezone) {}

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints de perfil de usuario")
public class UserController {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    public UserController(UserRepository userRepository, InstitutionRepository institutionRepository) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @GetMapping("/profile")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Actualizar el nombre del usuario autenticado")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        try {
            if (request.fullName() == null || request.fullName().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El nombre no puede estar vacío"));
            }
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            user.setName(request.fullName().trim());
            User saved = userRepository.save(user);
            return ResponseEntity.ok(toProfileResponse(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toProfileResponse(User user) {
        String institutionName = null;
        if (user.getInstitutionId() != null) {
            Institution institution = institutionRepository.findById(user.getInstitutionId()).orElse(null);
            institutionName = institution != null ? institution.getName() : null;
        }
        return Map.of(
                "id", String.valueOf(user.getUserId()),
                "fullName", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "institutionName", institutionName != null ? institutionName : ""
        );
    }
}
