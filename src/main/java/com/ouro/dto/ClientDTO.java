package com.ouro.dto;

import com.ouro.entity.Client;
import jakarta.validation.constraints.NotNull;

public class ClientDTO {
    
    // Request DTO para crear cliente
    public static class CreateClientRequest {
        
        @NotNull(message = "User ID es requerido")
        private Integer userId;
        
        private String dateOfBirth; // Format: "yyyy-MM-dd HH:mm:ss"
        
        private String timeOfBirth; // Format: "HH:mm:ss"
        
        // Constructors
        public CreateClientRequest() {
        }
        
        // Getters and Setters
        public Integer getUserId() {
            return userId;
        }
        
        public void setUserId(Integer userId) {
            this.userId = userId;
        }
        
        public String getDateOfBirth() {
            return dateOfBirth;
        }
        
        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
        
        public String getTimeOfBirth() {
            return timeOfBirth;
        }
        
        public void setTimeOfBirth(String timeOfBirth) {
            this.timeOfBirth = timeOfBirth;
        }
    }
    
    // Request DTO para actualizar cliente
    public static class UpdateClientRequest {
        
        private String dateOfBirth;
        
        private String timeOfBirth;
        
        // Constructors
        public UpdateClientRequest() {
        }
        
        // Getters and Setters
        public String getDateOfBirth() {
            return dateOfBirth;
        }
        
        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
        
        public String getTimeOfBirth() {
            return timeOfBirth;
        }
        
        public void setTimeOfBirth(String timeOfBirth) {
            this.timeOfBirth = timeOfBirth;
        }
    }
    
    // Response DTO
    public static class ClientResponse {
        
        private Integer id;
        private Integer userId;
        private String userEmail;
        private String userFullName;
        private String userPhone;
        private String dateOfBirth;
        private String timeOfBirth;
        
        // Constructors
        public ClientResponse() {
        }
        
        public ClientResponse(Client client) {
            this.id = client.getId();
            this.userId = client.getUser().getId();
            this.userEmail = client.getUser().getEmail();
            this.userFullName = client.getUser().getFullName();
            this.userPhone = client.getUser().getPhone();
            this.dateOfBirth = client.getDateOfBirth() != null ? client.getDateOfBirth().toString() : null;
            this.timeOfBirth = client.getTimeOfBirth() != null ? client.getTimeOfBirth().toString() : null;
        }
        
        // Getters and Setters
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public Integer getUserId() {
            return userId;
        }
        
        public void setUserId(Integer userId) {
            this.userId = userId;
        }
        
        public String getUserEmail() {
            return userEmail;
        }
        
        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }
        
        public String getUserFullName() {
            return userFullName;
        }
        
        public void setUserFullName(String userFullName) {
            this.userFullName = userFullName;
        }
        
        public String getUserPhone() {
            return userPhone;
        }
        
        public void setUserPhone(String userPhone) {
            this.userPhone = userPhone;
        }
        
        public String getDateOfBirth() {
            return dateOfBirth;
        }
        
        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
        
        public String getTimeOfBirth() {
            return timeOfBirth;
        }
        
        public void setTimeOfBirth(String timeOfBirth) {
            this.timeOfBirth = timeOfBirth;
        }
    }
}
