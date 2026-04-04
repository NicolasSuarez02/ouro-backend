package com.ouro.controller;

import com.ouro.dto.TherapistDTO;
import com.ouro.exception.EmailVerificationException;
import com.ouro.service.StorageService;
import com.ouro.service.TherapistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final StorageService storageService;

    @Autowired
    public TherapistController(TherapistService therapistService, StorageService storageService) {
        this.therapistService = therapistService;
        this.storageService = storageService;
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
        List<TherapistDTO.TherapistResponse> therapists;

        if (specialty != null && !specialty.isEmpty()) {
            therapists = therapistService.getTherapistsBySpecialty(specialty);
        } else {
            therapists = therapistService.getAllTherapists();
        }

        return new ResponseEntity<>(therapists, HttpStatus.OK);
    }

    @GetMapping("/pending")
    public ResponseEntity<Object> getPendingTherapists(@RequestParam Integer adminUserId) {
        try {
            List<TherapistDTO.TherapistResponse> therapists = therapistService.getPendingTherapists(adminUserId);
            return new ResponseEntity<>(therapists, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Object> approveTherapist(
            @PathVariable Integer id,
            @RequestParam Integer adminUserId) {
        try {
            TherapistDTO.TherapistResponse response = therapistService.approveTherapist(id, adminUserId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Object> rejectTherapist(
            @PathVariable Integer id,
            @RequestParam Integer adminUserId) {
        try {
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
     * Sube una foto de perfil. Retorna la URL pública de la foto.
     * POST /api/therapists/upload-photo?userId=X
     */
    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadPhoto(
            @RequestPart("photo") MultipartFile photo,
            @RequestParam Integer userId) {
        try {
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
     * Sirve una foto de perfil almacenada en disco.
     * GET /api/therapists/photos/{filename}
     */
    @GetMapping("/photos/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> servePhoto(
            @PathVariable String filename) {
        try {
            org.springframework.core.io.Resource resource = storageService.cargarFoto(filename);
            String contentType = "image/jpeg";
            if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
            else if (filename.toLowerCase().endsWith(".webp")) contentType = "image/webp";
            else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
