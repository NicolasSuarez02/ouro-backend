package com.ouro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailDTO {
    
    public static class SendEmailRequest {
        
        @NotBlank(message = "Email destinatario es requerido")
        @Email(message = "Email destinatario debe ser válido")
        private String to;
        
        @NotBlank(message = "Asunto es requerido")
        @Size(max = 255, message = "Asunto no puede exceder 255 caracteres")
        private String subject;
        
        @NotBlank(message = "Contenido es requerido")
        private String body;
        
        private boolean isHtml = false;
        
        // Constructors
        public SendEmailRequest() {
        }
        
        public SendEmailRequest(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
        
        // Getters and Setters
        public String getTo() {
            return to;
        }
        
        public void setTo(String to) {
            this.to = to;
        }
        
        public String getSubject() {
            return subject;
        }
        
        public void setSubject(String subject) {
            this.subject = subject;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
        
        public boolean isHtml() {
            return isHtml;
        }
        
        public void setHtml(boolean html) {
            isHtml = html;
        }
    }
    
    public static class EmailResponse {
        
        private boolean success;
        private String message;
        
        // Constructors
        public EmailResponse() {
        }
        
        public EmailResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
