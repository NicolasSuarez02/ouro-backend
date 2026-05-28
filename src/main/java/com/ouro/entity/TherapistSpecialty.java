package com.ouro.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "therapist_specialty")
public class TherapistSpecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "min_booking_lead_hours", nullable = false)
    private Integer minBookingLeadHours = 1;

    @Column(name = "price_amount_cents", nullable = false)
    private Integer priceAmountCents = 0;

    public TherapistSpecialty() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Therapist getTherapist() { return therapist; }
    public void setTherapist(Therapist therapist) { this.therapist = therapist; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMinBookingLeadHours() { return minBookingLeadHours; }
    public void setMinBookingLeadHours(Integer minBookingLeadHours) { this.minBookingLeadHours = minBookingLeadHours; }

    public Integer getPriceAmountCents() { return priceAmountCents; }
    public void setPriceAmountCents(Integer priceAmountCents) { this.priceAmountCents = priceAmountCents; }
}
