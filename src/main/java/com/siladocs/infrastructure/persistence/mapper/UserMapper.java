package com.siladocs.infrastructure.persistence.mapper;

import com.siladocs.domain.model.User;
import com.siladocs.infrastructure.persistence.entity.UserEntity;

public class UserMapper {

    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getInstitutionId(),
                entity.getCreatedAt()
        );
    }

    public static UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return new UserEntity(
                domain.getUserId(),
                domain.getName(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getRole(),
                domain.getInstitutionId(),
                domain.getCreatedAt()
        );
    }
}
