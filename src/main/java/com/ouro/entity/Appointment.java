package com.ouro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "appointment")
public class Appointment {

    public enum AppointmentStatus {
        PENDING_PAYMENT, RESERVED, CANCELLED, COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.RESERVED;

    @Column(name = "price_amount_cents", nullable = false)
    private Integer priceAmountCents = 0;

    @Column(nullable = false, length = 3)
    private String currency = "ARS";

    // FK a payment (feature futura, nullable)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "zoom_meeting_id")
    private String zoomMeetingId;

    @Column(name = "zoom_join_url", columnDefinition = "TEXT")
    private String zoomJoinUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Nota: la columna en DB tiene typo "pdated_at"
    @Column(name = "pdated_at", nullable = false)
    private LocalDateTime updatedAt;

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
    public Appointment() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Therapist getTherapist() {
        return therapist;
    }

    public void setTherapist(Therapist therapist) {
        this.therapist = therapist;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Integer getPriceAmountCents() {
        return priceAmountCents;
    }

    public void setPriceAmountCents(Integer priceAmountCents) {
        this.priceAmountCents = priceAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getZoomMeetingId() {
        return zoomMeetingId;
    }

    public void setZoomMeetingId(String zoomMeetingId) {
        this.zoomMeetingId = zoomMeetingId;
    }

    public String getZoomJoinUrl() {
        return zoomJoinUrl;
    }

    public void setZoomJoinUrl(String zoomJoinUrl) {
        this.zoomJoinUrl = zoomJoinUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
