package com.ouro.service;

import com.ouro.dto.EmailDTO;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@ouro.com.ar}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private Resend resend;

    @PostConstruct
    public void init() {
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            resend = new Resend(resendApiKey);
        } else {
            log.warn("RESEND_API_KEY no configurado — emails deshabilitados");
        }
    }

    public EmailDTO.EmailResponse sendSimpleEmail(EmailDTO.SendEmailRequest request) {
        return send(request.getTo(), request.getSubject(), null, request.getBody());
    }

    public EmailDTO.EmailResponse sendHtmlEmail(EmailDTO.SendEmailRequest request) {
        return send(request.getTo(), request.getSubject(), request.getBody(), null);
    }

    public EmailDTO.EmailResponse sendEmail(EmailDTO.SendEmailRequest request) {
        if (request.isHtml()) {
            return sendHtmlEmail(request);
        } else {
            return sendSimpleEmail(request);
        }
    }

    public EmailDTO.EmailResponse sendVerificationEmail(String toEmail, String fullName, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verifica tu cuenta - Ouro";
        String body = String.format(
            """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4A90E2;">¡Bienvenido a Ouro, %s!</h2>
                    <p>Gracias por registrarte en nuestra plataforma.</p>
                    <p>Para completar tu registro y verificar tu cuenta, por favor haz click en el siguiente botón:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background-color: #4A90E2; color: white; padding: 12px 30px;
                                  text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">
                            Verificar mi cuenta
                        </a>
                    </div>
                    <p>O copia y pega este enlace en tu navegador:</p>
                    <p style="word-break: break-all; color: #4A90E2;">%s</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #666;">
                        Este enlace expirará en 24 horas por seguridad.
                    </p>
                    <p style="font-size: 12px; color: #666;">
                        Si no creaste esta cuenta, podés ignorar este mensaje.
                    </p>
                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 12px; color: #999; text-align: center;">Ouro - Tu bienestar es nuestra prioridad</p>
                </div>
            </body>
            </html>
            """,
            fullName, verificationUrl, verificationUrl
        );
        return send(toEmail, subject, body, null);
    }

    public EmailDTO.EmailResponse sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Restablecer contraseña - Ouro";
        String body = String.format(
            """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4A90E2;">Restablecer contraseña</h2>
                    <p>Hola %s,</p>
                    <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en Ouro.</p>
                    <p>Haz click en el siguiente botón para crear una nueva contraseña:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background-color: #4A90E2; color: white; padding: 12px 30px;
                                  text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">
                            Restablecer contraseña
                        </a>
                    </div>
                    <p>O copia y pega este enlace en tu navegador:</p>
                    <p style="word-break: break-all; color: #4A90E2;">%s</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #666;">
                        Este enlace expirará en 1 hora por seguridad.
                    </p>
                    <p style="font-size: 12px; color: #666;">
                        Si no solicitaste restablecer tu contraseña, podés ignorar este mensaje.
                    </p>
                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 12px; color: #999; text-align: center;">Ouro - Tu bienestar es nuestra prioridad</p>
                </div>
            </body>
            </html>
            """,
            fullName, resetUrl, resetUrl
        );
        return send(toEmail, subject, body, null);
    }

    public EmailDTO.EmailResponse sendAppointmentConfirmation(
            String therapistEmail,
            String clientName,
            String appointmentDate,
            String appointmentTime,
            String zoomLink) {

        String subject = "Nueva Cita Agendada - " + clientName;
        String body = String.format(
            """
            <html>
            <body>
                <h2>Nueva Cita Agendada</h2>
                <p>Hola,</p>
                <p>Se ha agendada una nueva cita:</p>
                <ul>
                    <li><strong>Cliente:</strong> %s</li>
                    <li><strong>Fecha:</strong> %s</li>
                    <li><strong>Hora:</strong> %s</li>
                    <li><strong>Link Zoom:</strong> <a href="%s">Unirse a la reunión</a></li>
                </ul>
                <p>Saludos,<br>Equipo Ouro</p>
            </body>
            </html>
            """,
            clientName, appointmentDate, appointmentTime, zoomLink
        );
        return send(therapistEmail, subject, body, null);
    }

    public void sendNewAppointmentNotificationToTherapist(
            String therapistEmail,
            String clientNombre,
            String clientEmail,
            String clientTelefono,
            String clientFechaNac,
            String clientHoraNac,
            String especialidad,
            String fechaTurno,
            String horaTurno) {

        String subject = "Nuevo turno confirmado – " + clientNombre;
        String birthInfo = (clientFechaNac != null && !clientFechaNac.isBlank())
                ? clientFechaNac + (clientHoraNac != null && !clientHoraNac.isBlank() ? " a las " + clientHoraNac + " hs" : "")
                : "No informado";

        String body = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4A90E2;">¡Nuevo turno confirmado!</h2>
                    <p>Hola, te informamos que se confirmó un nuevo turno en tu agenda.</p>
                    <h3 style="color: #555; border-bottom: 1px solid #eee; padding-bottom: 8px;">Datos del turno</h3>
                    <ul style="list-style: none; padding: 0;">
                        <li><strong>Fecha:</strong> %s</li>
                        <li><strong>Hora:</strong> %s hs</li>
                        %s
                    </ul>
                    <h3 style="color: #555; border-bottom: 1px solid #eee; padding-bottom: 8px; margin-top: 20px;">Datos del cliente</h3>
                    <ul style="list-style: none; padding: 0;">
                        <li><strong>Nombre:</strong> %s</li>
                        <li><strong>Email:</strong> %s</li>
                        <li><strong>Teléfono:</strong> %s</li>
                        <li><strong>Fecha y hora de nacimiento:</strong> %s</li>
                    </ul>
                    <p style="margin-top: 20px; color: #555;">Podés ver todos los detalles y acceder a la sesión desde tu panel en
                        <a href="https://www.ouro.com.ar/mis-turnos">ouro.com.ar/mis-turnos</a>.
                    </p>
                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 12px; color: #999; text-align: center;">Ouro – Tu bienestar es nuestra prioridad</p>
                </div>
            </body>
            </html>
            """,
                fechaTurno, horaTurno,
                (especialidad != null && !especialidad.isBlank())
                        ? "<li><strong>Tipo de sesión:</strong> " + especialidad + "</li>"
                        : "",
                clientNombre, clientEmail, clientTelefono, birthInfo
        );
        send(therapistEmail, subject, body, null);
    }

    public EmailDTO.EmailResponse sendAppointmentReminder(
            String recipientEmail,
            String recipientName,
            String appointmentDate,
            String appointmentTime,
            String zoomLink) {

        String subject = "Recordatorio de Cita - " + appointmentDate;
        String body = String.format(
            """
            <html>
            <body>
                <h2>Recordatorio de Cita</h2>
                <p>Hola %s,</p>
                <p>Te recordamos tu cita programada:</p>
                <ul>
                    <li><strong>Fecha:</strong> %s</li>
                    <li><strong>Hora:</strong> %s</li>
                    <li><strong>Link Zoom:</strong> <a href="%s">Unirse a la reunión</a></li>
                </ul>
                <p>¡Te esperamos!</p>
                <p>Saludos,<br>Equipo Ouro</p>
            </body>
            </html>
            """,
            recipientName, appointmentDate, appointmentTime, zoomLink
        );
        return send(recipientEmail, subject, body, null);
    }

    private EmailDTO.EmailResponse send(String to, String subject, String html, String text) {
        if (resend == null) {
            log.warn("Email no enviado (Resend no configurado): to={} subject={}", to, subject);
            return new EmailDTO.EmailResponse(false, "Email deshabilitado: RESEND_API_KEY no configurado");
        }
        try {
            CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                    .from("Ouro <" + fromEmail + ">")
                    .to(to)
                    .subject(subject);
            if (html != null) builder.html(html);
            if (text != null) builder.text(text);
            resend.emails().send(builder.build());
            log.info("Email enviado a {}", to);
            return new EmailDTO.EmailResponse(true, "Email enviado exitosamente");
        } catch (ResendException e) {
            log.error("Error al enviar email a {}: {}", to, e.getMessage());
            return new EmailDTO.EmailResponse(false, "Error al enviar email: " + e.getMessage());
        }
    }
}
