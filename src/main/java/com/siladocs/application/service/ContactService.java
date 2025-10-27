package com.siladocs.application.service;

import com.siladocs.domain.model.ContactRequest;
import com.siladocs.domain.repository.ContactRequestRepository;
import com.siladocs.infrastructure.web.dto.ContactRequestDto;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service // <-- Marca como bean de servicio
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactRequestRepository contactRepository;
    private final JavaMailSender mailSender;

    // Inyección de dependencias vía constructor
    public ContactService(ContactRequestRepository contactRepository, JavaMailSender mailSender) {
        this.contactRepository = contactRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void processContactRequest(ContactRequestDto dto) {
        String fullName = dto.getFirstName() + " " + dto.getLastName();
        ContactRequest request = new ContactRequest(
                dto.getInstitutionName(),
                fullName,
                dto.getEmail(),
                dto.getPhone(),
                dto.getMessage()
        );

        try {
            contactRepository.save(request);
            log.info("Solicitud de contacto guardada para: {}", dto.getEmail());
        } catch (Exception e) {
            log.error("Error al guardar la solicitud de contacto para {}: {}", dto.getEmail(), e.getMessage(), e); // Loguea el stack trace
            throw new RuntimeException("Error al guardar en la base de datos", e);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@siladocs.com"); // Reemplaza si es necesario
        message.setTo("u20181h198@upc.edu.pe");
        message.setSubject("Nueva Solicitud de Contacto Siladocs: " + dto.getInstitutionName());
        String text = String.format(
                "Nueva solicitud:\n\nInstitución: %s\nNombre: %s\nEmail: %s\nTeléfono: %s\n\nMensaje:\n%s",
                dto.getInstitutionName(), fullName, dto.getEmail(),
                dto.getPhone() != null ? dto.getPhone() : "N/A",
                dto.getMessage() != null ? dto.getMessage() : "N/A"
        );
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Email de notificación enviado para {}", dto.getEmail());
        } catch (MailException e) {
            log.error("Error al enviar email de notificación para {}: {}", dto.getEmail(), e.getMessage(), e); // Loguea el stack trace
            // Considera si fallar o no aquí. Por ahora, solo loguea.
        }
    }
}