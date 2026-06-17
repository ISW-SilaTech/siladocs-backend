package com.siladocs.application.service;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final Map<String, PasswordResetToken> resetTokens = new HashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private static class PasswordResetToken {
        String code;
        LocalDateTime expiresAt;

        PasswordResetToken(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public void sendResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String code = generateResetCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);
        resetTokens.put(email, new PasswordResetToken(code, expiresAt));

        sendResetEmail(email, user.getName(), code);
        log.info("Password reset code sent to: {}", email);
    }

    public void verifyResetCode(String email, String code) {
        PasswordResetToken token = resetTokens.get(email);

        if (token == null) {
            throw new IllegalArgumentException("No se encontró una solicitud de recuperación de contraseña");
        }

        if (token.isExpired()) {
            resetTokens.remove(email);
            throw new IllegalArgumentException("El código ha expirado");
        }

        if (!token.code.equals(code)) {
            throw new IllegalArgumentException("Código incorrecto");
        }
    }

    public void resetPassword(String email, String code, String newPassword) {
        verifyResetCode(email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetTokens.remove(email);
        log.info("Password reset successfully for: {}", email);
    }

    private String generateResetCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // Azure App Service bloquea el tráfico SMTP saliente (puertos 25/587) en
    // los planes que usamos, por lo que enviar el correo directamente con
    // JavaMailSender falla siempre. En vez de eso delegamos el envío real al
    // endpoint de Vercel (HTTPS, sin restricción de salida) que ya usa el
    // mismo SMTP de Gmail de forma confiable para el resto de correos.
    private void sendResetEmail(String email, String userName, String code) {
        try {
            Map<String, String> payload = Map.of(
                    "email", email,
                    "userName", userName,
                    "code", code
            );
            restTemplate.postForEntity(frontendBaseUrl + "/api/send-password-reset-email", payload, String.class);
            log.info("Password reset email dispatched to: {}", email);
        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Error enviando email de recuperación");
        }
    }

    public int getResetCodeExpiryMinutes() {
        return CODE_EXPIRY_MINUTES;
    }
}
