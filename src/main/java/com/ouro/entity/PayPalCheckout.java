package com.ouro.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Una orden de PayPal creada para reservar un turno. Guarda qué se intentó reservar,
 * así no dependemos de datos que vuelven desde PayPal.
 */
@Entity
@Table(name = "paypal_checkout")
public class PayPalCheckout {

    public enum Status {
        CREATED,     // orden creada, el cliente todavía no pagó
        CAPTURED,    // cobro completado, turno creado
        PENDING,     // cobro en revisión de PayPal; el turno ya está creado
        SLOT_TAKEN,  // el horario se ocupó antes de cobrar; no se cobró nada
        FAILED,      // PayPal rechazó el cobro
        DENIED       // un cobro PENDING terminó rechazado; el turno se canceló
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "paypal_order_id", length = 64, unique = true)
    private String paypalOrderId;

    @Column(name = "time_slot_id", nullable = false)
    private Integer timeSlotId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "therapist_id", nullable = false)
    private Integer therapistId;

    @Column(name = "specialty_name", length = 255)
    private String specialtyName;

    @Column(name = "amount_usd_cents", nullable = false)
    private Integer amountUsdCents;

    // Cuenta a la que se le cobró; queda fija aunque la terapeuta cambie su email después
    @Column(name = "payee_email", nullable = false, length = 255)
    private String payeeEmail;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.CREATED;

    @Column(name = "paypal_capture_id", length = 64)
    private String paypalCaptureId;

    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public PayPalCheckout() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPaypalOrderId() { return paypalOrderId; }
    public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }

    public Integer getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Integer timeSlotId) { this.timeSlotId = timeSlotId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTherapistId() { return therapistId; }
    public void setTherapistId(Integer therapistId) { this.therapistId = therapistId; }

    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String specialtyName) { this.specialtyName = specialtyName; }

    public Integer getAmountUsdCents() { return amountUsdCents; }
    public void setAmountUsdCents(Integer amountUsdCents) { this.amountUsdCents = amountUsdCents; }

    public String getPayeeEmail() { return payeeEmail; }
    public void setPayeeEmail(String payeeEmail) { this.payeeEmail = payeeEmail; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getPaypalCaptureId() { return paypalCaptureId; }
    public void setPaypalCaptureId(String paypalCaptureId) { this.paypalCaptureId = paypalCaptureId; }

    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
