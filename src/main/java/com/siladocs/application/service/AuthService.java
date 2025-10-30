package com.siladocs.application.service;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.PasswordResetToken; // 🔹 Importar (Necesitas crear esta entidad)
import com.siladocs.infrastructure.persistence.jparepository.PasswordResetTokenRepository; // 🔹 Importar (Necesitas crear este repo)
import com.siladocs.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value; // 🔹 Importar
import org.springframework.mail.SimpleMailMessage; // 🔹 Importar
import org.springframework.mail.javamail.JavaMailSender; // 🔹 Importar
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant; // 🔹 Importar
import java.time.temporal.ChronoUnit; // 🔹 Importar
import java.util.Optional;
import java.util.UUID; // 🔹 Importar

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 🔹 ----- NUEVAS DEPENDENCIAS ----- 🔹
    private final PasswordResetTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final String frontendResetUrl; // URL de tu frontend, ej: http://localhost:3000/reset-password

    // ⬇️ 🔹 CONSTRUCTOR ACTUALIZADO 🔹
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       PasswordResetTokenRepository tokenRepo, // 🔹 Añadido
                       JavaMailSender mailSender,             // 🔹 Añadido
                       @Value("${app.frontend.reset-url}") String frontendResetUrl) { // 🔹 Añadido
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
        this.frontendResetUrl = frontendResetUrl;
    }

    // ---------------------------------------------
    // --- MÉTODOS EXISTENTES (Sin cambios) ---
    // ---------------------------------------------

    @Transactional
    public void registerAdmin(String name, String email, String rawPassword, Long institutionId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }
        User admin = new User(
                name,
                email,
                passwordEncoder.encode(rawPassword),
                "ROLE_ADMIN",
                institutionId
        );
        userRepository.save(admin);
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        return jwtUtil.generateToken(user.getEmail());
    }

    public void registerAdmin(String name, String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }
        User admin = new User(
                name,
                email,
                passwordEncoder.encode(rawPassword),
                "ROLE_ADMIN",
                null
        );
        userRepository.save(admin);
    }

    // ---------------------------------------------
    // --- 🔹 NUEVOS MÉTODOS PARA RESETEO 🔹 ---
    // ---------------------------------------------

    /**
     * Flujo 1: Usuario solicita restablecimiento.
     * Genera un token y envía un email.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // 1. Validar que el usuario exista usando el repositorio de dominio
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se encontró usuario con ese email."));

        // 2. Invalidar tokens viejos (usa el ID del modelo de dominio)
        tokenRepo.deleteByUserId(user.getUserId());

        // 3. Crear nuevo token
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(1, ChronoUnit.HOURS); // 1 hora de validez

        // 4. Guardar el token (asumiendo que PasswordResetToken almacena 'Long userId')
        PasswordResetToken resetToken = new PasswordResetToken(token, user.getUserId(), expiryDate);
        tokenRepo.save(resetToken);

        // 5. Construir enlace y enviar email
        String resetLink = frontendResetUrl + "?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@siladocs.com");
        message.setTo(user.getEmail());
        message.setSubject("Restablece tu contraseña de Siladocs");
        message.setText("Hola " + user.getName() + ",\n\n" +
                "Para restablecer tu contraseña, haz clic en el siguiente enlace:\n" + resetLink + "\n\n" +
                "Este enlace expira en 1 hora.\n\n" +
                "Gracias,\nEl equipo de Siladocs");

        mailSender.send(message);
    }

    /**
     * Flujo 2: Usuario ejecuta el restablecimiento.
     * Valida el token y actualiza la contraseña.
     */
    @Transactional
    public void performPasswordReset(String token, String newPassword) {
        // 1. Validar el token
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado."));

        // 2. Validar expiración
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepo.delete(resetToken); // Limpiar token expirado
            throw new RuntimeException("El token ha expirado.");
        }

        // 3. Obtener el usuario (del dominio) usando el ID del token
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario asociado al token no encontrado."));

        // 4. Actualizar contraseña en el modelo de dominio
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 5. Invalidar el token (¡Importante!)
        tokenRepo.delete(resetToken);
    }
}