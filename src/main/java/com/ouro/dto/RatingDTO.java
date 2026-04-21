package com.ouro.dto;

import com.ouro.entity.Rating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RatingDTO {

    // Request para crear calificación
    public static class CreateRatingRequest {

        @NotNull(message = "ID de terapeuta es requerido")
        private Integer therapistId;

        @NotNull(message = "Puntaje es requerido")
        @Min(value = 1, message = "Puntaje mínimo es 1")
        @Max(value = 5, message = "Puntaje máximo es 5")
        private Integer score;

        private String comment;

        public CreateRatingRequest() {
        }

        public Integer getTherapistId() {
            return therapistId;
        }

        public void setTherapistId(Integer therapistId) {
            this.therapistId = therapistId;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    // Respuesta de una calificación individual
    public static class RatingResponse {

        private Integer id;
        private Integer therapistId;
        private Integer userId;
        private String userFullName;
        private Integer score;
        private String comment;
        private String createdAt;

        public RatingResponse() {
        }

        public RatingResponse(Rating rating) {
            this.id = rating.getId();
            this.therapistId = rating.getTherapist().getId();
            this.userId = rating.getUser().getId();
            this.userFullName = rating.getUser().getFullName();
            this.score = rating.getScore();
            this.comment = rating.getComment();
            this.createdAt = rating.getCreatedAt() != null ? rating.getCreatedAt().toString() : null;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public Integer getTherapistId() { return therapistId; }
        public void setTherapistId(Integer therapistId) { this.therapistId = therapistId; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getUserFullName() { return userFullName; }
        public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // Estado del usuario respecto a calificación de un terapeuta
    public static class RatingEstado {

        private boolean puedeCalificar;
        private RatingResponse calificacionExistente;

        public RatingEstado() {
        }

        public RatingEstado(boolean puedeCalificar, RatingResponse calificacionExistente) {
            this.puedeCalificar = puedeCalificar;
            this.calificacionExistente = calificacionExistente;
        }

        public boolean isPuedeCalificar() { return puedeCalificar; }
        public void setPuedeCalificar(boolean puedeCalificar) { this.puedeCalificar = puedeCalificar; }

        public RatingResponse getCalificacionExistente() { return calificacionExistente; }
        public void setCalificacionExistente(RatingResponse calificacionExistente) { this.calificacionExistente = calificacionExistente; }
    }
}
