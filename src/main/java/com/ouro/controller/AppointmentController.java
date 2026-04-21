package com.ouro.controller;

import com.ouro.dto.AppointmentDTO;
import com.ouro.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Días del mes con al menos un slot libre para el terapeuta (público). */
    @GetMapping("/available-days")
    public ResponseEntity<List<String>> getAvailableDays(
            @RequestParam Integer therapistId,
            @RequestParam int year,
            @RequestParam int month) {
        List<String> dias = appointmentService.getDiasDisponiblesEnMes(therapistId, year, month);
        return new ResponseEntity<>(dias, HttpStatus.OK);
    }

    /** Slots libres de un terapeuta para un día específico (público). */
    @GetMapping("/available-slots")
    public ResponseEntity<List<AppointmentDTO.SlotResponse>> getAvailableSlots(
            @RequestParam Integer therapistId,
            @RequestParam String date) {
        LocalDate fecha = LocalDate.parse(date);
        List<AppointmentDTO.SlotResponse> slots =
                appointmentService.getSlotsDisponiblesPorDia(therapistId, fecha);
        return new ResponseEntity<>(slots, HttpStatus.OK);
    }

    /** Reserva un turno — requiere auth. El userId viene del JWT. */
    @PostMapping
    public ResponseEntity<Object> reservarTurno(
            @Valid @RequestBody AppointmentDTO.BookAppointmentRequest request) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.reservarTurno(request, userId);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /** Obtiene un turno por ID — solo el dueño, el terapeuta o un admin. */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getTurnoPorId(@PathVariable Integer id) {
        try {
            Integer requestingUserId = currentUserId();
            AppointmentDTO.AppointmentResponse response =
                    appointmentService.getTurnoPorId(id, requestingUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /** Cancela un turno — solo el usuario o el terapeuta del turno. */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Object> cancelarTurno(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.cancelarTurno(id, userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /** Marca un turno como completado — solo el terapeuta del turno. */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Object> completarTurno(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.completarTurno(id, userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /** Agenda de un usuario — solo él mismo o un admin. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> getTurnosPorUsuario(@PathVariable Integer userId) {
        try {
            Integer requestingUserId = currentUserId();
            AppointmentDTO.AgendaResponse response =
                    appointmentService.getTurnosPorUsuario(userId, requestingUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /** Agenda de un terapeuta — solo el propio terapeuta o un admin. */
    @GetMapping("/therapist/{therapistId}")
    public ResponseEntity<Object> getTurnosPorTerapeuta(@PathVariable Integer therapistId) {
        try {
            Integer requestingUserId = currentUserId();
            AppointmentDTO.AgendaResponse response =
                    appointmentService.getTurnosPorTerapeuta(therapistId, requestingUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
