package com.ouro.dto;

import com.ouro.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {
    
    // Request DTO para crear usuario
    public static class CreateUserRequest {
        
        @NotBlank(message = "Email es requerido")
        @Email(message = "Email debe ser válido")
        @Size(max = 320, message = "Email no puede exceder 320 caracteres")
        private String email;
        
        @NotBlank(message = "Nombre completo es requerido")
        @Size(max = 255, message = "Nombre no puede exceder 255 caracteres")
        private String fullName;
        
        @NotBlank(message = "Teléfono es requerido")
        @Size(max = 32, message = "Teléfono no puede exceder 32 caracteres")
        private String phone;
        
        @NotBlank(message = "Contraseña es requerida")
        @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
        private String password;
        
        private User.Role role;
        
        // Constructors
        public CreateUserRequest() {
        }
        
        public CreateUserRequest(String email, String fullName, String phone, String password) {
            this.email = email;
            this.fullName = fullName;
            this.phone = phone;
            this.password = password;
        }
        
        // Getters and Setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public User.Role getRole() {
            return role;
        }
        
        public void setRole(User.Role role) {
            this.role = role;
        }
    }
    
    // Request DTO para actualizar usuario
    public static class UpdateUserRequest {
        
        @Email(message = "Email debe ser válido")
        @Size(max = 320, message = "Email no puede exceder 320 caracteres")
        private String email;
        
        @Size(max = 255, message = "Nombre no puede exceder 255 caracteres")
        private String fullName;
        
        @Size(max = 32, message = "Teléfono no puede exceder 32 caracteres")
        private String phone;
        
        @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
        private String password;
        
        private User.Role role;
        
        // Constructors
        public UpdateUserRequest() {
        }
        
        // Getters and Setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public User.Role getRole() {
            return role;
        }
        
        public void setRole(User.Role role) {
            this.role = role;
        }
    }
    
    // Response DTO
    public static class UserResponse {
        
        private Integer id;
        private String email;
        private String fullName;
        private String phone;
        private String role;
        private Boolean emailVerified;
        private String createdAt;
        private String updatedAt;
        
        // Constructors
        public UserResponse() {
        }
        
        public UserResponse(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.phone = user.getPhone();
            this.role = user.getRole().name();
            this.emailVerified = user.getEmailVerified();
            this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
            this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;
        }
        
        // Getters and Setters
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public Boolean getEmailVerified() {
            return emailVerified;
        }
        
        public void setEmailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
        
        public String getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
    
    // Response extendido para panel de administración
    public static class UserAdminResponse extends UserResponse {

        private String therapistApprovalStatus;
        private String therapistSpecialty;
        private Integer therapistId;

        public UserAdminResponse() {
        }

        public UserAdminResponse(User user) {
            super(user);
            if (user.getTherapist() != null) {
                this.therapistId = user.getTherapist().getId();
                this.therapistApprovalStatus = user.getTherapist().getApprovalStatus() != null
                        ? user.getTherapist().getApprovalStatus().name() : null;
                this.therapistSpecialty = user.getTherapist().getSpecialty();
            }
        }

        public String getTherapistApprovalStatus() { return therapistApprovalStatus; }
        public void setTherapistApprovalStatus(String therapistApprovalStatus) { this.therapistApprovalStatus = therapistApprovalStatus; }

        public String getTherapistSpecialty() { return therapistSpecialty; }
        public void setTherapistSpecialty(String therapistSpecialty) { this.therapistSpecialty = therapistSpecialty; }

        public Integer getTherapistId() { return therapistId; }
        public void setTherapistId(Integer therapistId) { this.therapistId = therapistId; }
    }

    // DTO para verificación de email
    public static class VerifyEmailRequest {

        @NotBlank(message = "Token es requerido")
        private String token;

        public VerifyEmailRequest() {
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    // DTO para login
    public static class LoginRequest {

        @NotBlank(message = "Email es requerido")
        @Email(message = "Email debe ser válido")
        private String email;

        @NotBlank(message = "Contraseña es requerida")
        private String password;

        public LoginRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // DTO para solicitar reset de contraseña
    public static class ForgotPasswordRequest {

        @NotBlank(message = "Email es requerido")
        @Email(message = "Email debe ser válido")
        private String email;

        public ForgotPasswordRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // DTO para confirmar el reset de contraseña
    public static class ResetPasswordRequest {

        @NotBlank(message = "Token es requerido")
        private String token;

        @NotBlank(message = "Nueva contraseña es requerida")
        @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
        private String newPassword;

        public ResetPasswordRequest() {
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
