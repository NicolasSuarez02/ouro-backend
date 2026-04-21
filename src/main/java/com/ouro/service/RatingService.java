package com.ouro.service;

import com.ouro.dto.RatingDTO;
import com.ouro.entity.Appointment;
import com.ouro.entity.Rating;
import com.ouro.entity.Therapist;
import com.ouro.entity.User;
import com.ouro.repository.AppointmentRepository;
import com.ouro.repository.RatingRepository;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TherapistRepository therapistRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public RatingService(RatingRepository ratingRepository,
                         TherapistRepository therapistRepository,
                         UserRepository userRepository,
                         AppointmentRepository appointmentRepository) {
        this.ratingRepository = ratingRepository;
        this.therapistRepository = therapistRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Devuelve si el usuario puede calificar al terapeuta y su calificación existente.
     * Puede calificar si: tuvo al menos un turno COMPLETED con ese terapeuta y aún no lo calificó.
     */
    @Transactional(readOnly = true)
    public RatingDTO.RatingEstado obtenerEstado(Integer therapistId, Integer userId) {
        Optional<Rating> existente = ratingRepository.findByUserIdAndTherapistId(userId, therapistId);

        if (existente.isPresent()) {
            return new RatingDTO.RatingEstado(false, new RatingDTO.RatingResponse(existente.get()));
        }

        // Verificar si tiene algún turno completado con este terapeuta
        boolean tieneTurnoCompletado = appointmentRepository.findByUserId(userId).stream()
                .anyMatch(a -> a.getTherapist().getId().equals(therapistId)
                        && a.getStatus() == Appointment.AppointmentStatus.COMPLETED);

        return new RatingDTO.RatingEstado(tieneTurnoCompletado, null);
    }

    @Transactional
    public RatingDTO.RatingResponse crearCalificacion(RatingDTO.CreateRatingRequest request, Integer userId) {
        Therapist therapist = therapistRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // No puede calificar su propio perfil
        if (therapist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No podés calificar tu propio perfil");
        }

        // No puede calificar dos veces
        if (ratingRepository.existsByUserIdAndTherapistId(user.getId(), therapist.getId())) {
            throw new RuntimeException("Ya calificaste a este terapeuta");
        }

        // Debe tener al menos un turno completado
        boolean tieneTurnoCompletado = appointmentRepository.findByUserId(user.getId()).stream()
                .anyMatch(a -> a.getTherapist().getId().equals(therapist.getId())
                        && a.getStatus() == Appointment.AppointmentStatus.COMPLETED);

        if (!tieneTurnoCompletado) {
            throw new RuntimeException("Solo podés calificar a terapeutas con quienes tuviste un turno completado");
        }

        Rating rating = new Rating();
        rating.setTherapist(therapist);
        rating.setUser(user);
        rating.setScore(request.getScore());
        rating.setComment(request.getComment());

        return new RatingDTO.RatingResponse(ratingRepository.save(rating));
    }

    @Transactional(readOnly = true)
    public List<RatingDTO.RatingResponse> getCalificacionesPorTerapeuta(Integer therapistId) {
        return ratingRepository.findByTherapistIdOrderByCreatedAtDesc(therapistId).stream()
                .map(RatingDTO.RatingResponse::new)
                .collect(Collectors.toList());
    }
}
