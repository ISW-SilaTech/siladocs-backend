package com.siladocs.application.service;

import com.siladocs.domain.model.Institution;
import com.siladocs.domain.model.User;
// ¡Importamos el repositorio simple!
import com.siladocs.domain.repository.InstitutionRepository;
import com.siladocs.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    // Inyectamos los repositorios directos
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(InstitutionRepository institutionRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerNewInstitution(String instName, String instDomain, String adminName, String email, String password) {

        // La lógica es la misma, pero el código es más simple
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("El email ya está en uso");
        }

        if (institutionRepository.findByDomain(instDomain).isPresent()) {
            throw new IllegalStateException("El dominio ya está registrado");
        }

        // 2. Crear la entidad/modelo
        Institution newInstitution = new Institution(
                instName,
                instDomain,
                "PENDING"
        );

        // 3. Guardar directamente
        institutionRepository.save(newInstitution);

        // ... (resto del código para crear el User) ...
        String hashedPassword = passwordEncoder.encode(password);
        User adminUser = new User(
                adminName,
                email,
                hashedPassword,
                "ROLE_ADMIN",
                newInstitution.getInstitutionId()
        );

        return userRepository.save(adminUser);
    }
}