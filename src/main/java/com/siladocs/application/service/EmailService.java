package com.siladocs.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${contact.admin-email}")
    private String adminEmail;

    @Value("${contact.from-name:Siladocs}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendUserConfirmationEmail(String userEmail, String userName, String ticketId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject("Hemos recibido tu mensaje - Ticket #" + ticketId);

            String text = String.format(
                "Hola %s,\n\n" +
                "Agradecemos tu mensaje. Hemos creado un ticket de soporte para ti:\n\n" +
                "Número de Ticket: %s\n\n" +
                "Responderemos tu consulta en las próximas 24 horas.\n\n" +
                "Saludos,\n" +
                "%s",
                userName, ticketId, fromName
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Confirmation email sent to: {}", userEmail);
        } catch (Exception e) {
            log.error("Error sending confirmation email to {}: {}", userEmail, e.getMessage(), e);
        }
    }

    public void sendAdminNotificationEmail(String name, String email, String subject, String message, String ipAddress, String ticketId) {
        try {
            SimpleMailMessage notificationMessage = new SimpleMailMessage();
            notificationMessage.setFrom(fromEmail);
            notificationMessage.setTo(adminEmail);
            notificationMessage.setSubject("Nuevo Contacto: " + subject + " - Ticket #" + ticketId);

            String text = String.format(
                "Nuevo mensaje de contacto recibido:\n\n" +
                "Nombre: %s\n" +
                "Email: %s\n" +
                "Asunto: %s\n" +
                "Ticket: %s\n" +
                "IP: %s\n\n" +
                "Mensaje:\n%s\n\n" +
                "---\n" +
                "Responde a este mensaje para comunicarte con el usuario.",
                name, email, subject, ticketId, ipAddress, message
            );

            notificationMessage.setText(text);
            notificationMessage.setReplyTo(email);
            mailSender.send(notificationMessage);
            log.info("Admin notification email sent to: {}", adminEmail);
        } catch (Exception e) {
            log.error("Error sending admin notification email: {}", e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("HTML email sent to: {}", to);
    }
}
