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
            @RequestParam int month,
            @RequestParam(required = false) String specialty) {
        List<String> days = appointmentService.getAvailableDaysInMonth(therapistId, year, month, specialty);
        return new ResponseEntity<>(days, HttpStatus.OK);
    }

    /** Slots libres de un terapeuta para un día específico (público). */
    @GetMapping("/available-slots")
    public ResponseEntity<List<AppointmentDTO.SlotResponse>> getAvailableSlots(
            @RequestParam Integer therapistId,
            @RequestParam String date,
            @RequestParam(required = false) String specialty) {
        LocalDate parsedDate = LocalDate.parse(date);
        List<AppointmentDTO.SlotResponse> slots =
                appointmentService.getAvailableSlotsForDay(therapistId, parsedDate, specialty);
        return new ResponseEntity<>(slots, HttpStatus.OK);
    }

    /** Reserva un turno — requiere auth. El userId viene del JWT. */
    @PostMapping
    public ResponseEntity<Object> bookAppointment(
            @Valid @RequestBody AppointmentDTO.BookAppointmentRequest request) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.bookAppointment(request, userId);
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
    public ResponseEntity<Object> getAppointmentById(@PathVariable Integer id) {
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

    /** Regenera el link de pago para un turno PENDING_PAYMENT — solo el dueño del turno. */
    @GetMapping("/{id}/payment-link")
    public ResponseEntity<Object> getPaymentLink(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            String url = appointmentService.getPaymentLink(id, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("paymentUrl", url);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /** Cancela un turno — solo el usuario o el terapeuta del turno. */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Object> cancelAppointment(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.cancelAppointment(id, userId);
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
    public ResponseEntity<Object> completeAppointment(@PathVariable Integer id) {
        try {
            Integer userId = currentUserId();
            AppointmentDTO.AppointmentResponse response = appointmentService.completeAppointment(id, userId);
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
    public ResponseEntity<Object> getAppointmentsByUser(@PathVariable Integer userId) {
        try {
            Integer requestingUserId = currentUserId();
            AppointmentDTO.AgendaResponse response =
                    appointmentService.getAppointmentsByUser(userId, requestingUserId);
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
    public ResponseEntity<Object> getAppointmentsByTherapist(@PathVariable Integer therapistId) {
        try {
            Integer requestingUserId = currentUserId();
            AppointmentDTO.AgendaResponse response =
                    appointmentService.getAppointmentsByTherapist(therapistId, requestingUserId);
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
