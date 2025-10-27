package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.User;
import com.siladocs.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Convierte de Entidad (JPA) a Dominio (POJO)
    public User toDomain(UserEntity entity) {
        if (entity == null) return null;

        return new User(
                entity.getId(), // <--- CORRECCIÓN AQUÍ: Cambia getUserId() a getId()
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getInstitutionId(),
                entity.getCreatedAt()
        );
    }

    // Convierte de Dominio (POJO) a Entidad (JPA)
    public UserEntity toEntity(User domain) {
        if (domain == null) return null;

        // Asumiendo que User (dominio) sí tiene un método getUserId()
        return new UserEntity(
                domain.getUserId(), // <-- El campo en User (dominio) se llama userId
                domain.getName(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getRole(),
                domain.getInstitutionId(),
                domain.getCreatedAt()
        );
    }
}