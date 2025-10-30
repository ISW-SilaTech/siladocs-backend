package com.siladocs.application.controller;

import com.siladocs.application.dto.AuthResponse;
import com.siladocs.application.dto.LoginRequest;
import com.siladocs.application.dto.RegisterRequest;
import com.siladocs.application.service.AuthService;
// 🔹 Importa UserJpaRepository si lo usas en el login, o UserRepository si lo refactorizaste
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.domain.model.User; // 🔹 Importar User
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 🔹 DTOs (Records) para los nuevos endpoints
record ForgotPasswordRequest(String email) {}
record ResetPasswordRequest(String token, String newPassword) {}

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticación y registro")
public class AuthController {

    private final AuthService authService;
    // 🔹 Inyecta la interfaz limpia del dominio (UserRepository)
    private final UserRepository userRepo;

    // 🔹 Constructor actualizado
    public AuthController(AuthService authService, UserRepository userRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar administrador")
    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            if (request.institutionId() != null) {
                authService.registerAdmin(
                        request.name(), request.email(), request.password(), request.institutionId()
                );
            } else {
                authService.registerAdmin(request.name(), request.email(), request.password());
            }
            return ResponseEntity.ok("Administrador registrado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al registrar el administrador: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.email(), request.password());

            // 🔹 Usamos el repositorio de dominio
            User user = userRepo.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            AuthResponse response = new AuthResponse(
                    token, user.getEmail(), user.getRole(), user.getInstitutionId()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // ---------------------------------------------
    // 🔹 NUEVO ENDPOINT: Solicitar Restablecimiento
    // ---------------------------------------------
    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar restablecimiento de contraseña")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.requestPasswordReset(request.email());
            // Por seguridad, siempre devuelve OK
            return ResponseEntity.ok(Map.of("message", "Si el email está registrado, se ha enviado un enlace."));
        } catch (Exception e) {
            // Loguear el error, pero no revelarlo al usuario
            // log.error("Error en forgot-password: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Si el email está registrado, se ha enviado un enlace."));
        }
    }

    // ---------------------------------------------
    // 🔹 NUEVO ENDPOINT: Ejecutar Restablecimiento
    // ---------------------------------------------
    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer la contraseña")
    public ResponseEntity<?> performPasswordReset(@RequestBody ResetPasswordRequest request) {
        try {
            if (request.newPassword() == null || request.newPassword().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "La contraseña no puede estar vacía."));
            }

            authService.performPasswordReset(request.token(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente."));
        } catch (RuntimeException e) {
            // Errores como "Token inválido" o "Token expirado"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}