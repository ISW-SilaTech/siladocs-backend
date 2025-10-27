package com.siladocs.domain.repository;

import com.siladocs.domain.model.User;
import java.util.Optional;

// ⬇️ ¡ASEGÚRATE DE QUE ESTA LÍNEA NO TENGA "extends JpaRepository"!
public interface UserRepository {

    // Tu servicio de aplicación necesita estos dos métodos
    Optional<User> findByEmail(String email);

    User save(User user);
}