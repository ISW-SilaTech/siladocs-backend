package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.CareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Importa @Repository

import java.util.Optional;

@Repository // Aunque no es estrictamente necesario si escaneas el paquete, es buena práctica
public interface CareerJpaRepository extends JpaRepository<CareerEntity, Long> {
    // Spring Data JPA provee findAll, findById, save, deleteById, etc.

    // Método útil para validaciones
    boolean existsByName(String name);

    // Podrías añadir búsquedas personalizadas si necesitas
    // Optional<CareerEntity> findByName(String name);
}