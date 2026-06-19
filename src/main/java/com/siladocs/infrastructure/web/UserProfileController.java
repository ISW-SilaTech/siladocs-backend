package com.siladocs.infrastructure.web; // O com.siladocs.application.controller

import com.siladocs.application.service.ProfileService;
import com.siladocs.application.service.AzureBlobStorageService;
import com.siladocs.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

// DTOs (pueden ser records locales o archivos DTO separados)
record UpdateProfileRequest(String name) {}
record ProfileResponse(String name, String email, String role, Long institutionId) {}

@RestController
@RequestMapping("/profile")
public class UserProfileController { // Nota: Nombre cambiado de "ProfileController"

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    private final ProfileService profileService;
    private final AzureBlobStorageService azureBlobStorageService;

    public UserProfileController(ProfileService profileService, AzureBlobStorageService azureBlobStorageService) {
        this.profileService = profileService;
        this.azureBlobStorageService = azureBlobStorageService;
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

    /**
     * Endpoint para subir la foto de perfil (avatar) del usuario autenticado
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        try {
            // Validar que el archivo sea una imagen
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo debe ser una imagen"));
            }

            // Validar tamaño (máximo 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo no debe superar 5MB"));
            }

            String userEmail = authentication.getName();
            User user = profileService.getProfileByEmail(userEmail);

            // Crear nombre único para la imagen
            String fileName = String.format("avatars/%d_%s", user.getUserId(), System.currentTimeMillis() + "_" + file.getOriginalFilename());

            // Subir a Azure Blob Storage
            String avatarUrl = azureBlobStorageService.uploadBytes(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    fileName,
                    file.getContentType()
            );

            // Actualizar el perfil del usuario con la URL del avatar
            profileService.updateProfileAvatar(userEmail, avatarUrl);

            log.info("Avatar subido para usuario: {}", userEmail);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            log.error("Error al subir avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir la foto: " + e.getMessage()));
        }
    }
}