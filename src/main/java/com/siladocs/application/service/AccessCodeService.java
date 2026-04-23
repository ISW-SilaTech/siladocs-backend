package com.siladocs.application.service;

import com.siladocs.domain.entity.AccessCode;
import com.siladocs.domain.repository.AccessCodeRepository;
import com.siladocs.application.dto.GenerateCodeRequest; // Importante para el nuevo método
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AccessCodeService {

    private final AccessCodeRepository repository;

    public AccessCodeService(AccessCodeRepository repository) {
        this.repository = repository;
    }

    // ==========================================
    // 1. MÉTODO NUEVO: Generar Código (Para el Admin)
    // ==========================================
    @Transactional
    public AccessCode generateCode(GenerateCodeRequest request) {
        AccessCode newCode = new AccessCode();
        
        // Generamos un código aleatorio (Ej: SILA-A1B2C3)
        String randomString = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        newCode.setCode("SILA-" + randomString);
        
        // Asignamos la institución del DTO
        newCode.setInstitutionName(request.institutionName());

        // Caduca en 7 días y nace sin usar
        newCode.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        newCode.setUsed(false);

        return repository.save(newCode);
    }

    // ==========================================
    // 2. TUS MÉTODOS ACTUALES (Intactos)
    // ==========================================
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
