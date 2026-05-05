package com.ouro.controller;

import com.ouro.dto.TherapistDTO;
import com.ouro.exception.EmailVerificationException;
import com.ouro.service.MercadoPagoOAuthService;
import com.ouro.service.TherapistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/therapists")
@CrossOrigin(origins = "*")
public class TherapistController {

    private final TherapistService therapistService;
    private final MercadoPagoOAuthService mpOAuthService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public TherapistController(TherapistService therapistService,
                               MercadoPagoOAuthService mpOAuthService) {
        this.therapistService = therapistService;
        this.mpOAuthService = mpOAuthService;
    }

    @PostMapping
    public ResponseEntity<Object> createTherapist(
            @Valid @RequestBody TherapistDTO.CreateTherapistRequest request) {
        try {
            TherapistDTO.TherapistResponse response = therapistService.createTherapist(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (EmailVerificationException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("requiresEmailVerification", true);
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TherapistDTO.TherapistResponse> getTherapistById(@PathVariable Integer id) {
        try {
            TherapistDTO.TherapistResponse response = therapistService.getTherapistById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<TherapistDTO.TherapistResponse> getTherapistByUserId(@PathVariable Integer userId) {
        try {
            TherapistDTO.TherapistResponse response = therapistService.getTherapistByUserId(userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<TherapistDTO.TherapistResponse>> getAllTherapists(
            @RequestParam(required = false) String specialty) {
        List<TherapistDTO.TherapistResponse> therapists = (specialty != null && !specialty.isEmpty())
                ? therapistService.getTherapistsBySpecialty(specialty)
                : therapistService.getAllTherapists();
        return new ResponseEntity<>(therapists, HttpStatus.OK);
    }

    /**
     * GET /api/therapists/pending — solo para ADMIN (verificado por JWT).
     */
    @GetMapping("/pending")
    public ResponseEntity<Object> getPendingTherapists() {
        try {
            Integer adminUserId = currentUserId();
            List<TherapistDTO.TherapistResponse> therapists = therapistService.getPendingTherapists(adminUserId);
            return new ResponseEntity<>(therapists, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * PUT /api/therapists/{id}/approve — solo para ADMIN.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Object> approveTherapist(@PathVariable Integer id) {
        try {
            Integer adminUserId = currentUserId();
            TherapistDTO.TherapistResponse response = therapistService.approveTherapist(id, adminUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * PUT /api/therapists/{id}/reject — solo para ADMIN.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Object> rejectTherapist(@PathVariable Integer id) {
        try {
            Integer adminUserId = currentUserId();
            TherapistDTO.TherapistResponse response = therapistService.rejectTherapist(id, adminUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TherapistDTO.TherapistResponse> updateTherapist(
            @PathVariable Integer id,
            @Valid @RequestBody TherapistDTO.UpdateTherapistRequest request) {
        try {
            TherapistDTO.TherapistResponse response = therapistService.updateTherapist(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTherapist(@PathVariable Integer id) {
        try {
            therapistService.deleteTherapist(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * POST /api/therapists/upload-photo — requiere auth (JWT).
     */
    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadPhoto(@RequestPart("photo") MultipartFile photo) {
        try {
            Integer userId = currentUserId();
            String url = therapistService.uploadPhoto(userId, photo);
            Map<String, String> result = new HashMap<>();
            result.put("photoUrl", url);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GET /api/therapists/mp-connect — retorna la URL de autorización de Mercado Pago OAuth.
     * Requiere JWT (el userId se extrae del token).
     */
    @GetMapping("/mp-connect")
    public ResponseEntity<Object> getMpConnectUrl() {
        try {
            Integer userId = currentUserId();
            String authUrl = mpOAuthService.generarUrlAutorizacion(userId);
            Map<String, String> result = new HashMap<>();
            result.put("authUrl", authUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * GET /api/therapists/mp-callback — callback de Mercado Pago OAuth.
     * MP redirige aquí con code + state (userId). Guarda el token y redirige al frontend.
     */
    @GetMapping("/mp-callback")
    public ResponseEntity<Void> mpCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            Integer userId = Integer.parseInt(state);
            mpOAuthService.procesarCallback(code, userId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", frontendUrl + "/dashboard?mp=success")
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", frontendUrl + "/dashboard?mp=error")
                    .build();
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
