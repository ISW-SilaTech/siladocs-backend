package com.siladocs.application.service;

// ⬇️ Importa la interfaz limpia y el modelo de dominio
import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;

// import com.siladocs.infrastructure.persistence.entity.UserEntity; // No necesitamos la entidad aquí
// import com.siladocs.infrastructure.persistence.jparepository.UserJpaRepository; // No inyectamos esto
import com.siladocs.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional; // Necesario para Optional<User>

@Service
public class AuthService {

    // ⬇️ Cambia el tipo del repositorio
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ⬇️ Ajusta el constructor
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void registerAdmin(String name, String email, String rawPassword, Long institutionId) {
        // Usa la interfaz limpia y el modelo de dominio
        if (userRepository.findByEmail(email).isPresent()) { // findByEmail ahora devuelve Optional<User>
            throw new RuntimeException("El correo ya está registrado");
        }
        // Usa el constructor del modelo de dominio
        User admin = new User(
                name,
                email,
                passwordEncoder.encode(rawPassword),
                "ROLE_ADMIN",
                institutionId
        );
        userRepository.save(admin); // save ahora recibe y devuelve User
    }

    // ---------- Login ----------
    public String login(String email, String rawPassword) {
        // Usa la interfaz limpia y el modelo de dominio
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // Generamos JWT solo con el email como subject
        return jwtUtil.generateToken(user.getEmail());
    }

    // ---------- Registro simple (sin institutionId) ----------
    // Ajusta según necesites, este es un ejemplo si tu modelo User lo permite
    public void registerAdmin(String name, String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }
        // Necesitarías un constructor en User que no pida institutionId o manejarlo
        User admin = new User(
                name,
                email,
                passwordEncoder.encode(rawPassword),
                "ROLE_ADMIN",
                null // O algún valor por defecto si aplica
        );
        userRepository.save(admin);
    }
}