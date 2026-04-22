package com.siladocs.application.service;

import com.siladocs.domain.entity.AccessCode;
import com.siladocs.domain.repository.AccessCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessCodeService {

    private final AccessCodeRepository repository;

    public AccessCodeService(AccessCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AccessCode validateCode(String code) {

        AccessCode accessCode = repository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Código inválido"));

        if (accessCode.isExpired()) {
            throw new RuntimeException("El código ha expirado");
        }

        if (accessCode.isUsed()) {
            throw new RuntimeException("El código ya fue utilizado");
        }

        return accessCode;
    }

    @Transactional
    public void markAsUsed(AccessCode accessCode) {
        AccessCode entity = repository.findById(accessCode.getId())
                .orElseThrow(() -> new RuntimeException("Código no encontrado"));

        entity.setUsed(true);
        repository.save(entity);
    }
}
