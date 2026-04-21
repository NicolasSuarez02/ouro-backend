package com.ouro.dto;

import com.ouro.entity.Availability;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AvailabilityDTO {

    // Request para guardar/reemplazar toda la disponibilidad de un terapeuta
    public static class SaveAvailabilityRequest {

        @NotNull(message = "Los slots son requeridos")
        private List<SlotRequest> slots;

        public SaveAvailabilityRequest() {
        }

        public List<SlotRequest> getSlots() {
            return slots;
        }

        public void setSlots(List<SlotRequest> slots) {
            this.slots = slots;
        }
    }

    // Un slot de disponibilidad semanal
    public static class SlotRequest {

        // 0=Domingo, 1=Lunes, ..., 6=Sábado
        @NotNull(message = "dayOfWeek es requerido")
        private Integer dayOfWeek;

        // Formato "HH:mm"
        @NotNull(message = "startTime es requerido")
        private String startTime;

        // Formato "HH:mm"
        @NotNull(message = "endTime es requerido")
        private String endTime;

        private Integer slotDurationMinutes = 30;

        public SlotRequest() {
        }

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
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

        public Integer getSlotDurationMinutes() {
            return slotDurationMinutes;
        }

        public void setSlotDurationMinutes(Integer slotDurationMinutes) {
            this.slotDurationMinutes = slotDurationMinutes;
        }
    }

    // Response de un registro de disponibilidad
    public static class AvailabilityResponse {

        private Integer id;
        private Integer therapistId;
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;
        private Integer slotDurationMinutes;

        public AvailabilityResponse() {
        }

        public AvailabilityResponse(Availability availability) {
            this.id = availability.getId();
            this.therapistId = availability.getTherapist().getId();
            this.dayOfWeek = availability.getDayOfWeek();
            this.startTime = availability.getStartTime().toString();
            this.endTime = availability.getEndTime().toString();
            this.slotDurationMinutes = availability.getSlotDurationMinutes();
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

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
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

        public Integer getSlotDurationMinutes() {
            return slotDurationMinutes;
        }

        public void setSlotDurationMinutes(Integer slotDurationMinutes) {
            this.slotDurationMinutes = slotDurationMinutes;
        }
    }
}
