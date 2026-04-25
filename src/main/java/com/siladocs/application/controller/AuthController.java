package com.siladocs.application.controller;

import com.siladocs.application.dto.AuthResponse;
import com.siladocs.application.dto.LoginRequest;
import com.siladocs.application.dto.RegisterRequest;
import com.siladocs.application.dto.ValidateCodeRequest;
import com.siladocs.application.dto.InstitutionRegisterRequest;
import com.siladocs.application.service.AccessCodeService;
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

import com.siladocs.domain.repository.InstitutionRepository;

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
    private final AccessCodeService accessCodeService;
    private final InstitutionRepository institutionRepo;

    // 🔹 2. Constructor actualizado
    public AuthController(AuthService authService,
                          UserRepository userRepo,
                          AuthenticationManager authenticationManager,
                          AccessCodeService accessCodeService,
                          InstitutionRepository institutionRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
        this.authenticationManager = authenticationManager;
        this.accessCodeService = accessCodeService;
        this.institutionRepo = institutionRepo;
    }

    // ⬇️ 🔹 CAMBIADO A GET MAPPING Y REFACTORIZADO 🔹 ⬇️
    @GetMapping("/validate-code")
    @Operation(summary = "Validar código de acceso institucional")
    public ResponseEntity<?> validateCode(@RequestParam String code) {
        try {
            // Guardamos el resultado en una variable para extraer el nombre
            var accessCode = accessCodeService.validateCode(code);
            
            // Devolvemos el mensaje Y el nombre de la institución
            return ResponseEntity.ok(Map.of(
                    "message", "Código válido",
                    "institutionName", accessCode.getInstitutionName()
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registro institucional con código de acceso")
    public ResponseEntity<AuthResponse> registerInstitution(
            @RequestBody @jakarta.validation.Valid InstitutionRegisterRequest request) {

        String token = authService.registerInstitution(
                request.accessCode(),
                request.fullName(),
                request.email(),
                request.password()
        );

        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String institutionName = "Institución sin asignar";
        if (user.getInstitutionId() != null) {
            institutionName = institutionRepo.findById(user.getInstitutionId())
                    .map(inst -> inst.getName())
                    .orElse("Institución no encontrada");
        }

        AuthResponse response = new AuthResponse(
                token,
                new AuthUserDto(user.getUserId().toString(), user.getEmail(), user.getRole()),
                new AuthInstitutionDto(user.getInstitutionId() != null ? user.getInstitutionId().toString() : "", institutionName)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado")
    public ResponseEntity<?> me(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ⬅️ Buscamos el nombre de la institución dinámicamente
        String institutionName = "Institución sin asignar";
        if (user.getInstitutionId() != null) {
            institutionName = institutionRepo.findById(user.getInstitutionId())
                    .map(inst -> inst.getName()) // Asumiendo que tu entidad Institution tiene getName()
                    .orElse("Institución no encontrada");
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "email", user.getEmail(),
                "fullName", user.getName(),
                "role", user.getRole(),
                "institutionId", user.getInstitutionId() != null ? user.getInstitutionId() : "",
                "institutionName", institutionName // ⬅️ AHORA ENVIAMOS EL NOMBRE
        ));
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

            String institutionName = "Institución sin asignar";
            if (user.getInstitutionId() != null) {
                institutionName = institutionRepo.findById(user.getInstitutionId())
                        .map(inst -> inst.getName())
                        .orElse("Institución no encontrada");
            }

            AuthResponse response = new AuthResponse(
                    token,
                    new AuthUserDto(user.getUserId().toString(), user.getEmail(), user.getRole()),
                    new AuthInstitutionDto(user.getInstitutionId() != null ? user.getInstitutionId().toString() : "", institutionName)
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
