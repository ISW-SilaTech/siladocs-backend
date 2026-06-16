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
        String html = String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#0f172a 0%%,#1e3a5f 100%%);border-radius:16px 16px 0 0;padding:32px 40px;text-align:center">
                        <div style="font-size:26px;font-weight:700;color:#ffffff;letter-spacing:-0.5px">
                          Siladocs
                        </div>
                        <div style="margin-top:6px;display:inline-block;background:rgba(71,103,237,0.25);border:1px solid rgba(71,103,237,0.5);color:#93c5fd;border-radius:20px;padding:4px 14px;font-size:11px;font-weight:700;letter-spacing:2px">
                          ACCESO APROBADO
                        </div>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="background:#ffffff;padding:40px">

                        <p style="font-size:20px;font-weight:700;color:#0f172a;margin:0 0 8px">¡Tu solicitud fue aprobada!</p>
                        <p style="color:#64748b;margin:0 0 28px;line-height:1.6">
                          Hola <strong style="color:#0f172a">%s</strong>, el equipo de Siladocs revisó tu solicitud y ha sido aprobada.
                          A continuación encontrarás tu código de acceso único para completar el registro de tu institución.
                        </p>

                        <!-- Code box -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px">
                          <tr>
                            <td style="background:#f0f4ff;border:2px dashed #4767ed;border-radius:14px;padding:28px;text-align:center">
                              <p style="font-size:11px;color:#64748b;letter-spacing:2px;text-transform:uppercase;margin:0 0 12px;font-weight:600">Código de Acceso</p>
                              <span style="font-size:36px;font-weight:800;letter-spacing:6px;color:#4767ed;display:block;margin-bottom:12px">%s</span>
                              <p style="font-size:12px;color:#94a3b8;margin:0">
                                <span style="background:#fef3c7;color:#92400e;border-radius:6px;padding:3px 10px;font-weight:600">
                                  Válido por 7 días
                                </span>
                              </p>
                            </td>
                          </tr>
                        </table>

                        <p style="color:#475569;margin:0 0 20px;line-height:1.6">
                          Haz clic en el botón para ir directamente a la página de registro. El código ya estará ingresado automáticamente.
                        </p>

                        <!-- CTA Button -->
                        <table cellpadding="0" cellspacing="0" style="margin-bottom:32px">
                          <tr>
                            <td style="background:linear-gradient(135deg,#4767ed,#7b5cff);border-radius:10px">
                              <a href="%s" style="display:inline-block;color:#ffffff;text-decoration:none;padding:14px 36px;font-size:15px;font-weight:700;letter-spacing:0.3px">
                                Completar mi Registro →
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- Steps -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:10px;padding:20px;margin-bottom:24px">
                          <tr><td>
                            <p style="font-size:13px;font-weight:700;color:#334155;margin:0 0 12px">¿Cómo completar tu registro?</p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr>
                                <td style="padding:6px 0">
                                  <span style="display:inline-block;background:#4767ed;color:#fff;border-radius:50%%;width:22px;height:22px;text-align:center;line-height:22px;font-size:11px;font-weight:700;margin-right:10px">1</span>
                                  <span style="color:#475569;font-size:13px">Haz clic en el botón de arriba o copia el código</span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:6px 0">
                                  <span style="display:inline-block;background:#4767ed;color:#fff;border-radius:50%%;width:22px;height:22px;text-align:center;line-height:22px;font-size:11px;font-weight:700;margin-right:10px">2</span>
                                  <span style="color:#475569;font-size:13px">Valida el código en el formulario de registro</span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:6px 0">
                                  <span style="display:inline-block;background:#4767ed;color:#fff;border-radius:50%%;width:22px;height:22px;text-align:center;line-height:22px;font-size:11px;font-weight:700;margin-right:10px">3</span>
                                  <span style="color:#475569;font-size:13px">Completa tus datos y crea tu contraseña</span>
                                </td>
                              </tr>
                            </table>
                          </td></tr>
                        </table>

                        <p style="font-size:12px;color:#94a3b8;margin:0;line-height:1.6">
                          Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                          <span style="color:#4767ed;word-break:break-all">%s</span>
                        </p>

                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#0f172a;border-radius:0 0 16px 16px;padding:24px 40px;text-align:center">
                        <p style="color:#475569;font-size:12px;margin:0 0 4px">Siladocs — Gestión documental y trazabilidad blockchain de sílabos</p>
                        <p style="color:#334155;font-size:11px;margin:0">Si no solicitaste este acceso, ignora este correo.</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """, recipientName, accessCode, signUpUrl, signUpUrl);
        sendHtmlEmail(toEmail, "✅ Tu código de acceso a Siladocs está listo", html);
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
