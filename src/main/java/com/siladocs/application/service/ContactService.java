package com.siladocs.application.service;

// 🔹 Importa la ENTIDAD, no el modelo de dominio
import com.siladocs.infrastructure.persistence.entity.ContactRequestEntity;
// 🔹 Importa el REPOSITORIO JPA
import com.siladocs.infrastructure.persistence.jparepository.ContactRequestRepository;
import com.siladocs.infrastructure.web.dto.ContactRequestDto;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactRequestRepository contactRepository;
    private final Optional<JavaMailSender> mailSender;

    public ContactService(ContactRequestRepository contactRepository, Optional<JavaMailSender> mailSender) {
        this.contactRepository = contactRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void processContactRequest(ContactRequestDto dto) {
        String fullName = dto.getFirstName() + " " + dto.getLastName();

        // 🔹 Crea la ENTIDAD (ContactRequestEntity)
        ContactRequestEntity request = new ContactRequestEntity(
                dto.getInstitutionName(),
                fullName,
                dto.getEmail(),
                dto.getPhone(),
                dto.getMessage()
        );

        try {
            contactRepository.save(request); // 🔹 Guarda la Entidad
            log.info("Solicitud de contacto guardada para: {}", dto.getEmail());
        } catch (Exception e) {
            log.error("Error al guardar la solicitud de contacto para {}: {}", dto.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Error al guardar en la base de datos", e);
        }

        // ... (El resto de la lógica de envío de email está perfecta) ...
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@siladocs.com");
        message.setTo("u20181h198@upc.edu.pe");
        message.setSubject("Nueva Solicitud de Contacto Siladocs: " + dto.getInstitutionName());
        String text = String.format(
                "Nueva solicitud:\n\nInstitución: %s\nNombre: %s\nEmail: %s\nTeléfono: %s\n\nMensaje:\n%s",
                dto.getInstitutionName(), fullName, dto.getEmail(),
                dto.getPhone() != null ? dto.getPhone() : "N/A",
                dto.getMessage() != null ? dto.getMessage() : "N/A"
        );
        message.setText(text);

        // Enviar email solo si JavaMailSender está disponible
        mailSender.ifPresent(sender -> {
            try {
                sender.send(message);
                log.info("Email de notificación enviado para {}", dto.getEmail());
            } catch (MailException e) {
                log.error("Error al enviar email de notificación para {}: {}", dto.getEmail(), e.getMessage(), e);
            }
        });
    }
}