package com.ouro.controller;

import com.ouro.dto.RatingDTO;
import com.ouro.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")

public class RatingController {

    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /**
     * POST /api/ratings — requiere auth. El userId viene del JWT.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRating(
            @Valid @RequestBody RatingDTO.CreateRatingRequest request) {
        try {
            Integer userId = currentUserId();
            RatingDTO.RatingResponse response = ratingService.createRating(request, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rating", response);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/ratings/therapist/{therapistId}/estado — requiere auth. El userId viene del JWT.
     */
    @GetMapping("/therapist/{therapistId}/estado")
    public ResponseEntity<RatingDTO.RatingEstado> getStatus(
            @PathVariable Integer therapistId) {
        Integer userId = currentUserId();
        RatingDTO.RatingEstado status = ratingService.getStatus(therapistId, userId);
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/ratings/therapist/{therapistId} — público.
     */
    @GetMapping("/therapist/{therapistId}")
    public ResponseEntity<List<RatingDTO.RatingResponse>> getRatings(
            @PathVariable Integer therapistId) {
        return ResponseEntity.ok(ratingService.getRatingsByTherapist(therapistId));
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
