package com.siladocs.domain.repository;

import com.siladocs.domain.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, UUID> {
}
