package com.siladocs.domain.repository;

import com.siladocs.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    // Tu servicio de aplicación necesita estos dos métodos
    Optional<User> findByEmail(String email);

    User save(User user);

    // ⬇️ CORRECCIÓN AQUÍ
    Optional<User> findById(Long userId); // <-- Debe devolver Optional<User>, no Optional<Object>
}