package com.siladocs.application.controller;

import com.siladocs.application.dto.AuthResponse;
import com.siladocs.application.dto.LoginRequest;
import com.siladocs.application.dto.RegisterRequest;
import com.siladocs.application.service.AuthService;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// 🔹 Importaciones de Spring Security
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
// ---
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// DTOs (Records) para los nuevos endpoints
record ForgotPasswordRequest(String email) {}
record ResetPasswordRequest(String token, String newPassword) {}
// 🔹 DTO para el endpoint de "cambiar contraseña" (del perfil)
record ChangePasswordRequest(String currentPassword, String newPassword) {}


@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticación y registro")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;
    // 🔹 1. Inyecta el AuthenticationManager
    private final AuthenticationManager authenticationManager;

    // 🔹 2. Constructor actualizado
    public AuthController(AuthService authService, UserRepository userRepo, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.userRepo = userRepo;
        this.authenticationManager = authenticationManager;
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

    // ⬇️ 🔹 3. ENDPOINT /login CORREGIDO 🔹 ⬇️
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 1. Usa el AuthenticationManager para validar las credenciales
            // Esto llamará a tu método loadUserByUsername y verificará la contraseña
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // 2. Si la autenticación (arriba) no falló, generamos el token
            // Tu método login ahora solo necesita generar el token
            String token = authService.login(request.email(), request.password());

            // 3. Busca el usuario para la respuesta
            User user = userRepo.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            AuthResponse response = new AuthResponse(
                    token, user.getEmail(), user.getRole(), user.getInstitutionId()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) { // Captura (BadCredentialsException, etc.)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // ... (Tu endpoint /forgot-password está bien) ...
    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar restablecimiento de contraseña")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.requestPasswordReset(request.email());
            return ResponseEntity.ok(Map.of("message", "Si el email está registrado, se ha enviado un enlace."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Si el email está registrado, se ha enviado un enlace."));
        }
    }

    // ... (Tu endpoint /reset-password está bien) ...
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // ⬇️ 🔹 4. ENDPOINT /change-password AÑADIDO 🔹 ⬇️
    @PostMapping("/change-password")
    @Operation(summary = "Cambiar la contraseña (requiere autenticación)",
            description = "Permite al usuario actual cambiar su contraseña proveyendo la actual.")
    public ResponseEntity<?> changePassword(Authentication authentication, // Obtiene el usuario del token
                                            @RequestBody ChangePasswordRequest request) {
        try {
            // Obtiene el email del usuario autenticado
            String userEmail = authentication.getName();

            authService.changePassword(
                    userEmail,
                    request.currentPassword(),
                    request.newPassword()
            );
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente."));
        } catch (RuntimeException e) {
            // Errores como "Contraseña actual incorrecta"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}