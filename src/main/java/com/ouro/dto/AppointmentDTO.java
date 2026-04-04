package com.ouro.dto;

import com.ouro.entity.Appointment;
import com.ouro.entity.TimeSlot;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AppointmentDTO {

    // Request para reservar un turno
    public static class BookAppointmentRequest {

        @NotNull(message = "timeSlotId es requerido")
        private Integer timeSlotId;

        @NotNull(message = "userId es requerido")
        private Integer userId;

        private String notes;

        public BookAppointmentRequest() {
        }

        public Integer getTimeSlotId() {
            return timeSlotId;
        }

        public void setTimeSlotId(Integer timeSlotId) {
            this.timeSlotId = timeSlotId;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    // Response de un turno
    public static class AppointmentResponse {

        private Integer id;
        private Integer therapistId;
        private String therapistFullName;
        private Integer userId;
        private String userFullName;
        private String clientFullName;
        private String clientEmail;
        private String clientPhone;
        private String startAt;
        private String endAt;
        private String status;
        private Integer priceAmountCents;
        private String currency;
        private String notes;
        private String createdAt;

        public AppointmentResponse() {
        }

        public AppointmentResponse(Appointment appointment) {
            this.id = appointment.getId();
            this.therapistId = appointment.getTherapist().getId();
            this.therapistFullName = appointment.getTherapist().getUser().getFullName();
            this.userId = appointment.getUser().getId();
            this.userFullName = appointment.getUser().getFullName();
            this.clientFullName = appointment.getUser().getFullName();
            this.clientEmail = appointment.getUser().getEmail();
            this.clientPhone = appointment.getUser().getPhone();
            this.startAt = appointment.getStartAt() != null ? appointment.getStartAt().toString() : null;
            this.endAt = appointment.getEndAt() != null ? appointment.getEndAt().toString() : null;
            this.status = appointment.getStatus() != null ? appointment.getStatus().name() : null;
            this.priceAmountCents = appointment.getPriceAmountCents();
            this.currency = appointment.getCurrency();
            this.notes = appointment.getNotes();
            this.createdAt = appointment.getCreatedAt() != null ? appointment.getCreatedAt().toString() : null;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getTherapistId() {
            return therapistId;
        }

        public void setTherapistId(Integer therapistId) {
            this.therapistId = therapistId;
        }

        public String getTherapistFullName() {
            return therapistFullName;
        }

        public void setTherapistFullName(String therapistFullName) {
            this.therapistFullName = therapistFullName;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public String getUserFullName() {
            return userFullName;
        }

        public void setUserFullName(String userFullName) {
            this.userFullName = userFullName;
        }

        public String getClientFullName() {
            return clientFullName;
        }

        public void setClientFullName(String clientFullName) {
            this.clientFullName = clientFullName;
        }

        public String getClientEmail() {
            return clientEmail;
        }

        public void setClientEmail(String clientEmail) {
            this.clientEmail = clientEmail;
        }

        public String getClientPhone() {
            return clientPhone;
        }

        public void setClientPhone(String clientPhone) {
            this.clientPhone = clientPhone;
        }

        public String getStartAt() {
            return startAt;
        }

        public void setStartAt(String startAt) {
            this.startAt = startAt;
        }

        public String getEndAt() {
            return endAt;
        }

        public void setEndAt(String endAt) {
            this.endAt = endAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
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

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    // Agenda particionada en próximos y pasados (calculado en el service)
    public static class AgendaResponse {

        private List<AppointmentResponse> proximos;
        private List<AppointmentResponse> pasados;

        public AgendaResponse(List<AppointmentResponse> proximos, List<AppointmentResponse> pasados) {
            this.proximos = proximos;
            this.pasados = pasados;
        }

        public List<AppointmentResponse> getProximos() {
            return proximos;
        }

        public List<AppointmentResponse> getPasados() {
            return pasados;
        }
    }

    // Slot disponible para mostrar en el calendario
    public static class SlotResponse {

        private Integer id;
        private String startTime;
        private String endTime;

        public SlotResponse() {
        }

        public SlotResponse(TimeSlot slot) {
            this.id = slot.getId();
            this.startTime = slot.getStartAt().toLocalTime().toString();
            this.endTime = slot.getEndAt().toLocalTime().toString();
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
