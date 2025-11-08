package com.siladocs.application.service;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import com.siladocs.infrastructure.persistence.entity.PasswordResetToken;
import com.siladocs.infrastructure.persistence.jparepository.PasswordResetTokenRepository;
import com.siladocs.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
// 🔹 Importaciones de Spring Security
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// ---
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections; // 🔹 Importar
import java.util.Optional;
import java.util.UUID;

@Service
// 🔹 1. Implementa la interfaz UserDetailsService
public class AuthService implements UserDetailsService {

    // ... (Tus dependencias existentes)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final String frontendResetUrl;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       PasswordResetTokenRepository tokenRepo,
                       JavaMailSender mailSender,
                       @Value("${app.frontend.reset-url}") String frontendResetUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
        this.frontendResetUrl = frontendResetUrl;
    }

    // ... (Tus métodos existentes: registerAdmin, login, changePassword, etc.) ...

    // ⬇️ 🔹 2. MÉTODO loadUserByUsername (NUEVO) 🔹 ⬇️
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }

    // (Aquí van tus otros métodos: registerAdmin, login, requestPasswordReset, etc.)
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
    // ... (etc.)
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

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se encontró usuario con ese email."));
        tokenRepo.deleteByUserId(user.getUserId());
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(1, ChronoUnit.HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, user.getUserId(), expiryDate);
        tokenRepo.save(resetToken);
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

    @Transactional
    public void performPasswordReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado."));
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepo.delete(resetToken);
            throw new RuntimeException("El token ha expirado.");
        }
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario asociado al token no encontrado."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepo.delete(resetToken);
    }

    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}