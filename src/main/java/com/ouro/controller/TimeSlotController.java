package com.ouro.controller;

import com.ouro.entity.Therapist;
import com.ouro.entity.TimeSlot;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeslots")
@CrossOrigin(origins = "*")
public class TimeSlotController {

    private final TimeSlotRepository timeSlotRepository;
    private final TherapistRepository therapistRepository;

    @Autowired
    public TimeSlotController(TimeSlotRepository timeSlotRepository,
                              TherapistRepository therapistRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.therapistRepository = therapistRepository;
    }

    /**
     * Devuelve los slots FREE próximos del terapeuta (60 días).
     * Solo puede consultarlos el propio terapeuta (validado contra JWT).
     */
    @GetMapping("/therapist/{therapistId}")
    public ResponseEntity<Object> getUpcomingSlots(@PathVariable Integer therapistId) {
        try {
            Integer userId = currentUserId();
            Therapist therapist = therapistRepository.findById(therapistId)
                    .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));

            if (!therapist.getUser().getId().equals(userId)) {
                throw new RuntimeException("No tenés permiso para ver estos turnos");
            }

            LocalDateTime desde = LocalDateTime.now();
            LocalDateTime hasta = desde.plusDays(60);

            List<TimeSlot> slots = timeSlotRepository
                    .findByTherapistIdAndStatusAndStartAtBetween(
                            therapistId, TimeSlot.SlotStatus.FREE, desde, hasta);

            List<Map<String, Object>> response = slots.stream()
                    .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                    .map(slot -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", slot.getId());
                        m.put("startAt", slot.getStartAt().toString());
                        m.put("endAt", slot.getEndAt().toString());
                        m.put("status", slot.getStatus().name());
                        return m;
                    })
                    .collect(Collectors.toList());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Cancela (elimina) un slot FREE.
     * Solo puede hacerlo el terapeuta dueño (validado contra JWT).
     */
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Object> deleteSlot(@PathVariable Integer slotId) {
        try {
            Integer userId = currentUserId();
            TimeSlot slot = timeSlotRepository.findById(slotId)
                    .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

            if (!slot.getTherapist().getUser().getId().equals(userId)) {
                throw new RuntimeException("No tenés permiso para cancelar este turno");
            }

            if (slot.getStatus() != TimeSlot.SlotStatus.FREE) {
                throw new RuntimeException("Solo se pueden cancelar turnos libres. Este turno ya está reservado.");
            }

            timeSlotRepository.deleteById(slotId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
