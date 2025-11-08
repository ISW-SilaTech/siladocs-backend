package com.siladocs.infrastructure.web; // O com.siladocs.application.controller

import com.siladocs.application.service.ProfileService;
import com.siladocs.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// DTOs (pueden ser records locales o archivos DTO separados)
record UpdateProfileRequest(String name) {}
record ProfileResponse(String name, String email, String role, Long institutionId) {}

@RestController
@RequestMapping("/api/profile")
public class UserProfileController { // Nota: Nombre cambiado de "ProfileController"

    private final ProfileService profileService;

    public UserProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Endpoint para obtener los datos del usuario autenticado (para la vista de Perfil)
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        // authentication.getName() nos da el email (username) del token JWT
        String userEmail = authentication.getName();
        User user = profileService.getProfileByEmail(userEmail);

        ProfileResponse response = new ProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getInstitutionId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para actualizar el nombre del usuario autenticado
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(Authentication authentication,
                                                           @RequestBody UpdateProfileRequest request) {
        String userEmail = authentication.getName();
        User updatedUser = profileService.updateProfileName(userEmail, request.name());

        ProfileResponse response = new ProfileResponse(
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getInstitutionId()
        );
        return ResponseEntity.ok(response);
    }
}