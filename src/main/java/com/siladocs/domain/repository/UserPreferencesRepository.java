package com.siladocs.domain.repository;

import com.siladocs.domain.model.UserPreferences;
import java.util.Optional;

public interface UserPreferencesRepository {

    UserPreferences save(UserPreferences preferences);
    Optional<UserPreferences> findById(Long id);
    Optional<UserPreferences> findByUserId(Long userId);
}
