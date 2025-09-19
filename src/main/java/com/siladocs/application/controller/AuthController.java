package com.siladocs.application.controller;

import com.siladocs.application.dto.AuthResponse;
import com.siladocs.application.dto.LoginRequest;
import com.siladocs.application.dto.RegisterRequest;
import com.siladocs.application.service.AuthService;
import com.siladocs.infrastructure.persistence.jparepository.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticación y registro")
public class AuthController {

    private final AuthService authService;
    private final UserJpaRepository userRepo;

    public AuthController(AuthService authService, UserJpaRepository userRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar administrador",
            description = "Registra un nuevo usuario administrador")
    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            if (request.institutionId() != null) {
                authService.registerAdmin(
                        request.name(),
                        request.email(),
                        request.password(),
                        request.institutionId()
                );
            } else {
                authService.registerAdmin(
                        request.name(),
                        request.email(),
                        request.password()
                );
            }
            return ResponseEntity.ok("Administrador registrado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al registrar el administrador: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve un JWT válido junto con datos del usuario si las credenciales son correctas")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Generamos el token JWT
            String token = authService.login(request.email(), request.password());

            // Obtenemos info del usuario para devolver en AuthResponse
            var userEntity = userRepo.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            AuthResponse response = new AuthResponse(
                    token,
                    userEntity.getEmail(),
                    userEntity.getRole(),
                    userEntity.getInstitutionId()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }
    }
}
