package com.siladocs.domain.repository;

import com.siladocs.domain.model.Institution;
import java.util.Optional;

// ⬇️ ¡ASEGÚRATE DE QUE ESTA LÍNEA NO TENGA "extends JpaRepository"!
public interface InstitutionRepository {

    // Tu servicio de aplicación solo necesita estos dos métodos
    Institution save(Institution institution);

    Optional<Institution> findByDomain(String domain);
}