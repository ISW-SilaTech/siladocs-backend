package com.siladocs.application.controller;

import com.siladocs.application.service.AzureBlobStorageService;
import com.siladocs.domain.model.Institution;
import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

record UpdateProfileRequest(String fullName, String language, String timezone) {}

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints de perfil de usuario")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> AVATAR_EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int AVATAR_SAS_EXPIRY_HOURS = 24 * 30; // 30 días

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final AzureBlobStorageService azureBlobStorageService;

    public UserController(UserRepository userRepository, InstitutionRepository institutionRepository,
                           AzureBlobStorageService azureBlobStorageService) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.azureBlobStorageService = azureBlobStorageService;
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

    @PostMapping("/profile/avatar")
    @Operation(summary = "Subir la foto de perfil (avatar) del usuario autenticado")
    public ResponseEntity<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo debe ser una imagen JPG, PNG o WEBP"));
            }
            if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo no debe superar 5MB"));
            }

            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String extension = AVATAR_EXTENSION_BY_CONTENT_TYPE.get(contentType);
            String blobName = String.format("avatars/%d.%s", user.getUserId(), extension);

            azureBlobStorageService.uploadBytes(file.getBytes(), file.getOriginalFilename(), blobName, contentType);
            String avatarUrl = azureBlobStorageService.generateDownloadSasUrl(blobName, AVATAR_SAS_EXPIRY_HOURS);

            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            log.info("Avatar actualizado para usuario: {}", authentication.getName());

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            log.error("Error al subir avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir la foto: " + e.getMessage()));
        }
    }

    private Map<String, Object> toProfileResponse(User user) {
        String institutionName = null;
        if (user.getInstitutionId() != null) {
            Institution institution = institutionRepository.findById(user.getInstitutionId()).orElse(null);
            institutionName = institution != null ? institution.getName() : null;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", String.valueOf(user.getUserId()));
        response.put("fullName", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("institutionName", institutionName != null ? institutionName : "");
        response.put("avatarUrl", user.getAvatarUrl());
        return response;
    }
}
