package com.siladocs.application.service;

import com.siladocs.application.dto.CreateRegistrationRequestDto;
import com.siladocs.application.dto.GenerateCodeRequest;
import com.siladocs.application.dto.ReviewRegistrationRequestDto;
import com.siladocs.domain.entity.AccessCode;
import com.siladocs.domain.entity.RegistrationRequest;
import com.siladocs.domain.repository.RegistrationRequestRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RegistrationRequestService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRequestService.class);

    private final RegistrationRequestRepository repository;
    private final AccessCodeService accessCodeService;
    private final EmailService emailService;

    @Value("${siladocs.frontend.reset-url:https://siladocs-frontend.vercel.app/authentication/sign-up/cover}")
    private String signUpUrl;

    public RegistrationRequestService(RegistrationRequestRepository repository,
                                      AccessCodeService accessCodeService,
                                      EmailService emailService) {
        this.repository = repository;
        this.accessCodeService = accessCodeService;
        this.emailService = emailService;
    }

    @Transactional
    public RegistrationRequest submit(CreateRegistrationRequestDto dto) {
        RegistrationRequest req = new RegistrationRequest();
        req.setFullName(dto.fullName());
        req.setEmail(dto.email());
        req.setInstitutionName(dto.institutionName());
        req.setMessage(dto.message());
        req.setStatus(RegistrationRequest.Status.PENDING);
        return repository.save(req);
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequest> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public RegistrationRequest findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
    }

    @Transactional
    public RegistrationRequest approve(UUID id, ReviewRegistrationRequestDto dto) {
        RegistrationRequest req = findById(id);
        if (req.getStatus() != RegistrationRequest.Status.PENDING) {
            throw new RuntimeException("La solicitud ya fue revisada");
        }

        AccessCode code = accessCodeService.generateCode(new GenerateCodeRequest(req.getInstitutionName()));

        req.setStatus(RegistrationRequest.Status.APPROVED);
        req.setReviewedAt(Instant.now());
        req.setReviewNote(dto != null ? dto.reviewNote() : null);
        repository.save(req);

        try {
            emailService.sendAccessCodeEmail(req.getEmail(), req.getFullName(), code.getCode(), buildSignUpUrl(code.getCode()));
        } catch (Exception e) {
            log.error("Error sending access code email to {}: {}", req.getEmail(), e.getMessage());
        }

        return req;
    }

    @Transactional
    public RegistrationRequest reject(UUID id, ReviewRegistrationRequestDto dto) {
        RegistrationRequest req = findById(id);
        if (req.getStatus() != RegistrationRequest.Status.PENDING) {
            throw new RuntimeException("La solicitud ya fue revisada");
        }

        req.setStatus(RegistrationRequest.Status.REJECTED);
        req.setReviewedAt(Instant.now());
        req.setReviewNote(dto != null ? dto.reviewNote() : null);
        repository.save(req);

        try {
            emailService.sendRejectionEmail(req.getEmail(), req.getFullName(), req.getInstitutionName(),
                    dto != null ? dto.reviewNote() : null);
        } catch (Exception e) {
            log.error("Error sending rejection email to {}: {}", req.getEmail(), e.getMessage());
        }

        return req;
    }

    @Transactional
    public AccessCode sendCode(UUID id) {
        RegistrationRequest req = findById(id);
        if (req.getStatus() == RegistrationRequest.Status.REJECTED) {
            throw new RuntimeException("No se puede enviar código a una solicitud rechazada");
        }

        // Si estaba pendiente, aprobarla automáticamente
        if (req.getStatus() == RegistrationRequest.Status.PENDING) {
            req.setStatus(RegistrationRequest.Status.APPROVED);
            req.setReviewedAt(Instant.now());
            repository.save(req);
        }

        AccessCode code = accessCodeService.generateCode(new GenerateCodeRequest(req.getInstitutionName()));

        try {
            emailService.sendAccessCodeEmail(req.getEmail(), req.getFullName(), code.getCode(), buildSignUpUrl(code.getCode()));
        } catch (Exception e) {
            log.error("Error sending access code email to {}: {}", req.getEmail(), e.getMessage(), e);
            // No relanzamos: el código ya quedó generado y persistido; el admin puede
            // reintentar el envío del email sin perder el código. Igual que el patrón
            // usado en sendUserConfirmationEmail/sendAdminNotificationEmail.
        }

        return code;
    }

    private String buildSignUpUrl(String code) {
        String base = signUpUrl.contains("/authentication/sign-up")
                ? signUpUrl
                : "https://siladocs-frontend.vercel.app/authentication/sign-up/cover";
        return base + "?code=" + code;
    }
}
