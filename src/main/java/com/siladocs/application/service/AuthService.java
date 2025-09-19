package com.siladocs.application.service;

import com.siladocs.infrastructure.persistence.entity.UserEntity;
import com.siladocs.infrastructure.persistence.jparepository.UserJpaRepository;
import com.siladocs.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserJpaRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserJpaRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void registerAdmin(String name, String email, String rawPassword, Long institutionId) {
        if (userRepo.existsByEmail(email)) {
            throw new RuntimeException("El correo ya está registrado");
        }
        UserEntity admin = new UserEntity();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole("ROLE_ADMIN");
        admin.setInstitutionId(institutionId);
        userRepo.save(admin);
    }

    // ---------- Login ----------
    public String login(String email, String rawPassword) {
        UserEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // Generamos JWT solo con el email como subject
        return jwtUtil.generateToken(user.getEmail());
    }

    // ---------- Registro simple ----------
    public void registerAdmin(String name, String email, String rawPassword) {
        if (userRepo.existsByEmail(email)) {
            throw new RuntimeException("El correo ya está registrado");
        }
        UserEntity admin = new UserEntity();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole("ROLE_ADMIN");
        userRepo.save(admin);
    }
}
