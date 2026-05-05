package com.ouro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "user")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(nullable = false, length = 32)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Therapist therapist;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Client client;
    
    public enum Role {
        USER, THERAPIST, ADMIN
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
    
    // Constructors
    public User() {
    }
    
    public User(String email, String fullName, String phone, String password) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
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
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public String getVerificationToken() {
        return verificationToken;
    }
    
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
    
    public LocalDateTime getVerificationTokenExpiry() {
        return verificationTokenExpiry;
    }

    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) {
        this.verificationTokenExpiry = verificationTokenExpiry;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiry() {
        return resetPasswordTokenExpiry;
    }

    public void setResetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
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
    
    public Therapist getTherapist() {
        return therapist;
    }
    
    public void setTherapist(Therapist therapist) {
        this.therapist = therapist;
    }
    
    public Client getClient() {
        return client;
    }
    
    public void setClient(Client client) {
        this.client = client;
    }
}
