package com.siladocs.application.service;

import com.siladocs.domain.model.User;
import com.siladocs.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca un usuario por su email (obtenido del token).
     */
    @Transactional(readOnly = true)
    public User getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado (token inválido)"));
    }

    /**
     * Actualiza el nombre del usuario.
     */
    @Transactional
    public User updateProfileName(String email, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new RuntimeException("El nombre no puede estar vacío.");
        }

        User user = getProfileByEmail(email); // Reutiliza el método de búsqueda
        user.setName(newName);

        // El .save() de JPA detectará que el usuario ya tiene ID y hará un UPDATE
        return userRepository.save(user);
    }
}