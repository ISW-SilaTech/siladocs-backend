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

    public void sendAccessCodeEmail(String toEmail, String recipientName, String accessCode, String signUpUrl) throws MessagingException {
        String html = String.format(
            "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:24px'>" +
            "<h2 style='color:#1e3a5f'>¡Tu solicitud ha sido aprobada!</h2>" +
            "<p>Hola <strong>%s</strong>,</p>" +
            "<p>El equipo de Siladocs ha aprobado tu solicitud. Usa el siguiente código para completar tu registro:</p>" +
            "<div style='background:#f0f4ff;border:2px dashed #4767ed;border-radius:12px;padding:20px 24px;margin:24px 0;text-align:center'>" +
            "<p style='font-size:13px;color:#64748b;margin-bottom:8px;letter-spacing:1px;text-transform:uppercase'>Código de Acceso</p>" +
            "<span style='font-size:2rem;font-weight:700;letter-spacing:4px;color:#4767ed'>%s</span></div>" +
            "<p>Haz clic aquí para completar tu registro:</p>" +
            "<a href='%s' style='display:inline-block;background:linear-gradient(135deg,#4767ed,#7b5cff);color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:600;margin:8px 0'>Completar mi registro</a>" +
            "<p style='font-size:13px;color:#64748b;margin-top:24px'>El código es válido por 7 días.</p>" +
            "<p style='font-size:12px;color:#94a3b8'>Siladocs — Gestión documental y trazabilidad blockchain de sílabos</p></div>",
            recipientName, accessCode, signUpUrl
        );
        sendHtmlEmail(toEmail, "Tu código de acceso a Siladocs", html);
    }

    public void sendRejectionEmail(String toEmail, String recipientName, String institutionName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Actualización sobre tu solicitud de acceso a Siladocs");
            String text = String.format(
                "Hola %s,\n\nGracias por tu interés en Siladocs. Lamentablemente, no podemos aprobar " +
                "la solicitud para \"%s\" en este momento.\n\n%s\n\n" +
                "Contáctanos en contacto@siladocs.com si tienes preguntas.\n\nEl equipo de Siladocs",
                recipientName, institutionName,
                reason != null && !reason.isBlank() ? "Motivo: " + reason : ""
            );
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error sending rejection email to {}: {}", toEmail, e.getMessage());
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
