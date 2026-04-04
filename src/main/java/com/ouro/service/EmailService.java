package com.ouro.service;

import com.ouro.dto.EmailDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Enviar email simple (texto plano)
     */
    public EmailDTO.EmailResponse sendSimpleEmail(EmailDTO.SendEmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            
            mailSender.send(message);
            
            return new EmailDTO.EmailResponse(true, "Email enviado exitosamente a " + request.getTo());
        } catch (Exception e) {
            return new EmailDTO.EmailResponse(false, "Error al enviar email: " + e.getMessage());
        }
    }
    
    /**
     * Enviar email HTML
     */
    public EmailDTO.EmailResponse sendHtmlEmail(EmailDTO.SendEmailRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true); // true indica HTML
            
            mailSender.send(mimeMessage);
            
            return new EmailDTO.EmailResponse(true, "Email HTML enviado exitosamente a " + request.getTo());
        } catch (Exception e) {
            return new EmailDTO.EmailResponse(false, "Error al enviar email HTML: " + e.getMessage());
        }
    }
    
    /**
     * Enviar email (detecta automáticamente si es HTML o texto)
     */
    public EmailDTO.EmailResponse sendEmail(EmailDTO.SendEmailRequest request) {
        if (request.isHtml()) {
            return sendHtmlEmail(request);
        } else {
            return sendSimpleEmail(request);
        }
    }
    
    /**
     * Enviar email de verificación con token
     */
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
                           style="background-color: #4A90E2; 
                                  color: white; 
                                  padding: 12px 30px; 
                                  text-decoration: none; 
                                  border-radius: 5px; 
                                  display: inline-block;
                                  font-weight: bold;">
                            Verificar mi cuenta
                        </a>
                    </div>
                    <p>O copia y pega este enlace en tu navegador:</p>
                    <p style="word-break: break-all; color: #4A90E2;">%s</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #666;">
                        Este enlace expirará en 24 horas por seguridad.
                    </p>
                    <p style="font-size: 12px; color: #666;">
                        Si no creaste esta cuenta, puedes ignorar este mensaje.
                    </p>
                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 12px; color: #999; text-align: center;">
                        Ouro - Tu bienestar es nuestra prioridad
                    </p>
                </div>
            </body>
            </html>
            """,
            fullName, verificationUrl, verificationUrl
        );
        
        EmailDTO.SendEmailRequest request = new EmailDTO.SendEmailRequest(toEmail, subject, body);
        request.setHtml(true);
        
        return sendEmail(request);
    }
    
    /**
     * Enviar email de reset de contraseña
     */
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
                           style="background-color: #4A90E2;
                                  color: white;
                                  padding: 12px 30px;
                                  text-decoration: none;
                                  border-radius: 5px;
                                  display: inline-block;
                                  font-weight: bold;">
                            Restablecer contraseña
                        </a>
                    </div>
                    <p>O copia y pega este enlace en tu navegador:</p>
                    <p style="word-break: break-all; color: #4A90E2;">%s</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #666;">
                        Este enlace expirará en 1 hora por seguridad.
                    </p>
                    <p style="font-size: 12px; color: #666;">
                        Si no solicitaste restablecer tu contraseña, puedes ignorar este mensaje. Tu contraseña no cambiará.
                    </p>
                    <hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 12px; color: #999; text-align: center;">
                        Ouro - Tu bienestar es nuestra prioridad
                    </p>
                </div>
            </body>
            </html>
            """,
            fullName, resetUrl, resetUrl
        );

        EmailDTO.SendEmailRequest request = new EmailDTO.SendEmailRequest(toEmail, subject, body);
        request.setHtml(true);

        return sendEmail(request);
    }

    /**
     * Enviar email de confirmación de cita para terapeutas
     */
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
                <p>Se ha agendado una nueva cita:</p>
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
        
        EmailDTO.SendEmailRequest request = new EmailDTO.SendEmailRequest(therapistEmail, subject, body);
        request.setHtml(true);
        
        return sendEmail(request);
    }
    
    /**
     * Enviar email de recordatorio de cita
     */
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
                <p>Te esperamos!</p>
                <p>Saludos,<br>Equipo Ouro</p>
            </body>
            </html>
            """,
            recipientName, appointmentDate, appointmentTime, zoomLink
        );
        
        EmailDTO.SendEmailRequest request = new EmailDTO.SendEmailRequest(recipientEmail, subject, body);
        request.setHtml(true);
        
        return sendEmail(request);
    }
}
