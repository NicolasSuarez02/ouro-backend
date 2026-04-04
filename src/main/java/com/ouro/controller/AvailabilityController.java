package com.ouro.controller;

import com.ouro.dto.AvailabilityDTO;
import com.ouro.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = "*")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Autowired
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Reemplaza toda la disponibilidad semanal del terapeuta.
     * Solo puede hacerlo el propio terapeuta (userId debe coincidir con el terapeuta).
     */
    @PutMapping("/therapist/{therapistId}")
    public ResponseEntity<Object> saveAvailability(
            @PathVariable Integer therapistId,
            @Valid @RequestBody AvailabilityDTO.SaveAvailabilityRequest request) {
        try {
            List<AvailabilityDTO.AvailabilityResponse> response =
                    availabilityService.saveAvailability(therapistId, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Devuelve la configuración de disponibilidad de un terapeuta (público).
     */
    @GetMapping("/therapist/{therapistId}")
    public ResponseEntity<List<AvailabilityDTO.AvailabilityResponse>> getAvailability(
            @PathVariable Integer therapistId) {
        List<AvailabilityDTO.AvailabilityResponse> response =
                availabilityService.getAvailabilityByTherapist(therapistId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
