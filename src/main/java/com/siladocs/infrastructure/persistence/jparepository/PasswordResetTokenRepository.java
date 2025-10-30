package com.siladocs.infrastructure.persistence.jparepository;

import com.siladocs.infrastructure.persistence.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Método para encontrar un token por su string
    Optional<PasswordResetToken> findByToken(String token);

    // Método para borrar tokens viejos de un usuario
    @Transactional
    void deleteByUserId(Long userId);
}