package com.ouro.dto;

import com.ouro.entity.Therapist;
import com.ouro.entity.TherapistSpecialty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Collectors;

public class TherapistDTO {

    public static class SpecialtyDTO {
        private String name;
        private Integer minBookingLeadHours;
        private Integer priceAmountCents;

        public SpecialtyDTO() {}

        public SpecialtyDTO(TherapistSpecialty s) {
            this.name = s.getName();
            this.minBookingLeadHours = s.getMinBookingLeadHours();
            this.priceAmountCents = s.getPriceAmountCents();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getMinBookingLeadHours() { return minBookingLeadHours; }
        public void setMinBookingLeadHours(Integer minBookingLeadHours) { this.minBookingLeadHours = minBookingLeadHours; }
        public Integer getPriceAmountCents() { return priceAmountCents; }
        public void setPriceAmountCents(Integer priceAmountCents) { this.priceAmountCents = priceAmountCents; }
    }
    
    // Request DTO para crear terapeuta
    public static class CreateTherapistRequest {
        
        @NotNull(message = "User ID es requerido")
        private Integer userId;
        
        @Size(max = 5000, message = "Bio no puede exceder 5000 caracteres")
        private String bio;
        
        @Size(max = 255, message = "Especialidad no puede exceder 255 caracteres")
        private String specialty;
        
        @Size(max = 2048, message = "URL de foto no puede exceder 2048 caracteres")
        private String photoUrl;
        
        @Min(value = 0, message = "Precio no puede ser negativo")
        private Integer priceAmountCents;
        
        @Size(min = 3, max = 3, message = "Moneda debe tener 3 caracteres")
        private String priceCurrency;

        private String mpAccessToken;

        private Integer minBookingLeadHours;

        private List<SpecialtyDTO> specialties;

        // Constructors
        public CreateTherapistRequest() {
        }

        // Getters and Setters
        public Integer getUserId() {
            return userId;
        }
        
        public void setUserId(Integer userId) {
            this.userId = userId;
        }
        
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
        
        public String getSpecialty() {
            return specialty;
        }
        
        public void setSpecialty(String specialty) {
            this.specialty = specialty;
        }
        
        public String getPhotoUrl() {
            return photoUrl;
        }
        
        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }
        
        public Integer getPriceAmountCents() {
            return priceAmountCents;
        }
        
        public void setPriceAmountCents(Integer priceAmountCents) {
            this.priceAmountCents = priceAmountCents;
        }
        
        public String getPriceCurrency() {
            return priceCurrency;
        }
        
        public void setPriceCurrency(String priceCurrency) {
            this.priceCurrency = priceCurrency;
        }

        public String getMpAccessToken() {
            return mpAccessToken;
        }

        public void setMpAccessToken(String mpAccessToken) {
            this.mpAccessToken = mpAccessToken;
        }

        public Integer getMinBookingLeadHours() {
            return minBookingLeadHours;
        }

        public void setMinBookingLeadHours(Integer minBookingLeadHours) {
            this.minBookingLeadHours = minBookingLeadHours;
        }

        public List<SpecialtyDTO> getSpecialties() { return specialties; }
        public void setSpecialties(List<SpecialtyDTO> specialties) { this.specialties = specialties; }
    }

    // Request DTO para actualizar terapeuta
    public static class UpdateTherapistRequest {
        
        @Size(max = 5000, message = "Bio no puede exceder 5000 caracteres")
        private String bio;
        
        @Size(max = 255, message = "Especialidad no puede exceder 255 caracteres")
        private String specialty;
        
        @Size(max = 2048, message = "URL de foto no puede exceder 2048 caracteres")
        private String photoUrl;
        
        @Min(value = 0, message = "Precio no puede ser negativo")
        private Integer priceAmountCents;
        
        @Size(min = 3, max = 3, message = "Moneda debe tener 3 caracteres")
        private String priceCurrency;

        private String mpAccessToken;

        private Integer minBookingLeadHours;

        private List<SpecialtyDTO> specialties;

        // Constructors
        public UpdateTherapistRequest() {
        }

        // Getters and Setters
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
        
        public String getSpecialty() {
            return specialty;
        }
        
        public void setSpecialty(String specialty) {
            this.specialty = specialty;
        }
        
        public String getPhotoUrl() {
            return photoUrl;
        }
        
        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }
        
        public Integer getPriceAmountCents() {
            return priceAmountCents;
        }
        
        public void setPriceAmountCents(Integer priceAmountCents) {
            this.priceAmountCents = priceAmountCents;
        }
        
        public String getPriceCurrency() {
            return priceCurrency;
        }
        
        public void setPriceCurrency(String priceCurrency) {
            this.priceCurrency = priceCurrency;
        }

        public String getMpAccessToken() {
            return mpAccessToken;
        }

        public void setMpAccessToken(String mpAccessToken) {
            this.mpAccessToken = mpAccessToken;
        }

        public Integer getMinBookingLeadHours() {
            return minBookingLeadHours;
        }

        public void setMinBookingLeadHours(Integer minBookingLeadHours) {
            this.minBookingLeadHours = minBookingLeadHours;
        }

        public List<SpecialtyDTO> getSpecialties() { return specialties; }
        public void setSpecialties(List<SpecialtyDTO> specialties) { this.specialties = specialties; }
    }

    // Response DTO
    public static class TherapistResponse {

        private Integer id;
        private Integer userId;
        private String userEmail;
        private String userFullName;
        private String userPhone;
        private String bio;
        private String specialty;
        private String photoUrl;
        private Integer priceAmountCents;
        private String priceCurrency;
        private String approvalStatus;
        private String createdAt;
        private String updatedAt;
        // true si el terapeuta ya configuró su token de MP (no devolvemos el token en claro)
        private boolean mpTokenConfigurado;
        private Integer minBookingLeadHours;
        private String slug;
        private List<SpecialtyDTO> specialties;
        // Calificación promedio (1-5), null si no tiene calificaciones aún
        private Double averageRating;
        private Integer ratingCount;

        // Constructors
        public TherapistResponse() {
        }

        public TherapistResponse(Therapist therapist) {
            this.id = therapist.getId();
            this.userId = therapist.getUser().getId();
            this.userEmail = therapist.getUser().getEmail();
            this.userFullName = therapist.getUser().getFullName();
            this.userPhone = therapist.getUser().getPhone();
            this.bio = therapist.getBio();
            this.specialty = therapist.getSpecialty();
            this.photoUrl = therapist.getPhotoUrl();
            this.priceAmountCents = therapist.getPriceAmountCents();
            this.priceCurrency = therapist.getPriceCurrency();
            this.approvalStatus = therapist.getApprovalStatus() != null ? therapist.getApprovalStatus().name() : null;
            this.createdAt = therapist.getCreatedAt() != null ? therapist.getCreatedAt().toString() : null;
            this.updatedAt = therapist.getUpdatedAt() != null ? therapist.getUpdatedAt().toString() : null;
            this.mpTokenConfigurado = therapist.getMpAccessToken() != null && !therapist.getMpAccessToken().isBlank();
            this.minBookingLeadHours = therapist.getMinBookingLeadHours() != null ? therapist.getMinBookingLeadHours() : 1;
            this.slug = therapist.getSlug();
            this.specialties = therapist.getSpecialties() != null
                    ? therapist.getSpecialties().stream().map(SpecialtyDTO::new).collect(Collectors.toList())
                    : List.of();
        }

        public TherapistResponse(Therapist therapist, Double averageRating, long ratingCount) {
            this(therapist);
            this.averageRating = averageRating != null
                    ? Math.round(averageRating * 10.0) / 10.0
                    : null;
            this.ratingCount = (int) ratingCount;
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
        
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
        
        public String getSpecialty() {
            return specialty;
        }
        
        public void setSpecialty(String specialty) {
            this.specialty = specialty;
        }
        
        public String getPhotoUrl() {
            return photoUrl;
        }
        
        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }
        
        public Integer getPriceAmountCents() {
            return priceAmountCents;
        }
        
        public void setPriceAmountCents(Integer priceAmountCents) {
            this.priceAmountCents = priceAmountCents;
        }
        
        public String getPriceCurrency() {
            return priceCurrency;
        }

        public void setPriceCurrency(String priceCurrency) {
            this.priceCurrency = priceCurrency;
        }

        public String getApprovalStatus() {
            return approvalStatus;
        }

        public void setApprovalStatus(String approvalStatus) {
            this.approvalStatus = approvalStatus;
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

        public boolean isMpTokenConfigurado() {
            return mpTokenConfigurado;
        }

        public void setMpTokenConfigurado(boolean mpTokenConfigurado) {
            this.mpTokenConfigurado = mpTokenConfigurado;
        }

        public Integer getMinBookingLeadHours() {
            return minBookingLeadHours;
        }

        public void setMinBookingLeadHours(Integer minBookingLeadHours) {
            this.minBookingLeadHours = minBookingLeadHours;
        }

        public Double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(Double averageRating) {
            this.averageRating = averageRating;
        }

        public Integer getRatingCount() {
            return ratingCount;
        }

        public void setRatingCount(Integer ratingCount) {
            this.ratingCount = ratingCount;
        }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public List<SpecialtyDTO> getSpecialties() { return specialties; }
        public void setSpecialties(List<SpecialtyDTO> specialties) { this.specialties = specialties; }
    }
}
