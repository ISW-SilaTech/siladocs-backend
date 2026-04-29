package com.siladocs.application.service;

import com.siladocs.application.dto.ContactMessageListDto;
import com.siladocs.application.dto.ContactMessageRequest;
import com.siladocs.application.dto.ContactMessageResponse;
import com.siladocs.domain.model.ContactMessage;
import com.siladocs.domain.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContactMessageService {

    private static final Logger log = LoggerFactory.getLogger(ContactMessageService.class);

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;
    private final RecaptchaService recaptchaService;

    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                EmailService emailService,
                                RecaptchaService recaptchaService) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
        this.recaptchaService = recaptchaService;
    }

    @Transactional
    public ContactMessageResponse sendMessage(ContactMessageRequest request, String ipAddress, String userAgent) {
        // 1. Validar reCAPTCHA
        if (!recaptchaService.validateToken(request.recaptchaToken())) {
            log.warn("reCAPTCHA validation failed for email: {}", request.email());
            throw new RuntimeException("reCAPTCHA validation failed");
        }

        // 2. Crear el mensaje
        ContactMessage message = new ContactMessage(
            request.name(),
            request.email(),
            request.subject(),
            request.message()
        );

        if (request.phone() != null) {
            message.setPhone(request.phone());
        }

        if (request.company() != null) {
            message.setCompany(request.company());
        }

        message.setIpAddress(ipAddress);
        message.setUserAgent(userAgent);

        // 3. Guardar en BD
        ContactMessage savedMessage = contactMessageRepository.save(message);
        log.info("Contact message saved with ID: {}", savedMessage.getId());

        // 4. Enviar email de confirmación al usuario
        try {
            String ticketId = generateTicketId(savedMessage.getId());
            emailService.sendUserConfirmationEmail(request.email(), request.name(), ticketId);
            emailService.sendAdminNotificationEmail(request.name(), request.email(), request.subject(),
                request.message(), ipAddress, ticketId);
        } catch (Exception e) {
            log.error("Error sending emails: {}", e.getMessage(), e);
            // No re-throw - el mensaje fue guardado exitosamente
        }

        return ContactMessageResponse.fromDomain(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ContactMessageListDto> getAllMessages() {
        return contactMessageRepository.findAll()
            .stream()
            .map(ContactMessageListDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContactMessageListDto> getMessagesByStatus(String status) {
        return contactMessageRepository.findByStatus(status)
            .stream()
            .map(ContactMessageListDto::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ContactMessageResponse> getMessageById(UUID id) {
        return contactMessageRepository.findById(id)
            .map(message -> {
                // Marcar como leído
                if ("NEW".equals(message.getStatus().name())) {
                    message.setStatus(ContactMessage.MessageStatus.READ);
                    contactMessageRepository.save(message);
                }
                return ContactMessageResponse.fromDomain(message);
            });
    }

    @Transactional
    public void updateMessageStatus(UUID id, String status) {
        contactMessageRepository.updateStatus(id, status);
        log.info("Message {} status updated to: {}", id, status);
    }

    @Transactional
    public void deleteMessage(UUID id) {
        contactMessageRepository.delete(id);
        log.info("Message {} deleted", id);
    }

    private String generateTicketId(UUID messageId) {
        long timestamp = System.currentTimeMillis();
        return String.format("TKT-%d-%d", timestamp / 1000, messageId.hashCode() & 0xFFFF);
    }
}
