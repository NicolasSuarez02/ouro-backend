package com.ouro.entity;

import com.ouro.security.AesEncryptionConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "therapist")
public class Therapist {

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(length = 255)
    private String specialty;
    
    @Column(name = "photo_url", length = 2048)
    private String photoUrl;
    
    @Column(name = "price_amount_cents", nullable = false)
    private Integer priceAmountCents = 0;
    
    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency = "ARS";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "mp_access_token", length = 768)
    private String mpAccessToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Therapist() {
    }
    
    public Therapist(User user) {
        this.user = user;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getMpAccessToken() {
        return mpAccessToken;
    }

    public void setMpAccessToken(String mpAccessToken) {
        this.mpAccessToken = mpAccessToken;
    }
}
