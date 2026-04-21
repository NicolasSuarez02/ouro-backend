package com.ouro.service;

import com.ouro.dto.TherapistDTO;
import com.ouro.entity.Therapist;
import com.ouro.entity.User;
import com.ouro.exception.EmailVerificationException;
import com.ouro.repository.RatingRepository;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final RatingRepository ratingRepository;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Autowired
    public TherapistService(TherapistRepository therapistRepository,
                            UserRepository userRepository,
                            StorageService storageService,
                            RatingRepository ratingRepository) {
        this.therapistRepository = therapistRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.ratingRepository = ratingRepository;
    }

    private TherapistDTO.TherapistResponse toResponse(Therapist therapist) {
        Double avg = ratingRepository.findAverageScoreByTherapistId(therapist.getId());
        long count = ratingRepository.countByTherapistId(therapist.getId());
        return new TherapistDTO.TherapistResponse(therapist, avg, count);
    }
    
    @Transactional
    public TherapistDTO.TherapistResponse createTherapist(TherapistDTO.CreateTherapistRequest request) {
        // Verificar que el usuario existe
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + request.getUserId()));
        
        // Verificar que el email está verificado
        if (!user.getEmailVerified()) {
            throw new EmailVerificationException("Debe verificar su email antes de crear un perfil de terapeuta");
        }
        
        // Verificar que el usuario no tenga ya un perfil de terapeuta
        if (therapistRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("El usuario ya tiene un perfil de terapeuta");
        }
        
        // Actualizar rol del usuario a THERAPIST
        user.setRole(User.Role.THERAPIST);
        userRepository.save(user);
        
        Therapist therapist = new Therapist();
        therapist.setUser(user);
        therapist.setBio(request.getBio());
        therapist.setSpecialty(request.getSpecialty());
        therapist.setPhotoUrl(request.getPhotoUrl());
        
        if (request.getPriceAmountCents() != null) {
            therapist.setPriceAmountCents(request.getPriceAmountCents());
        }
        
        if (request.getPriceCurrency() != null) {
            therapist.setPriceCurrency(request.getPriceCurrency());
        }

        if (request.getMpAccessToken() != null && !request.getMpAccessToken().isBlank()) {
            therapist.setMpAccessToken(request.getMpAccessToken().trim());
        }

        Therapist savedTherapist = therapistRepository.save(therapist);
        return toResponse(savedTherapist);
    }

    @Transactional(readOnly = true)
    public TherapistDTO.TherapistResponse getTherapistById(Integer id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + id));
        return toResponse(therapist);
    }

    @Transactional(readOnly = true)
    public TherapistDTO.TherapistResponse getTherapistByUserId(Integer userId) {
        Therapist therapist = therapistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado para user id: " + userId));
        return toResponse(therapist);
    }

    @Transactional(readOnly = true)
    public List<TherapistDTO.TherapistResponse> getAllTherapists() {
        return therapistRepository.findByApprovalStatus(Therapist.ApprovalStatus.APPROVED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TherapistDTO.TherapistResponse> getPendingTherapists(Integer adminUserId) {
        verificarAdmin(adminUserId);
        return therapistRepository.findByApprovalStatus(Therapist.ApprovalStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TherapistDTO.TherapistResponse approveTherapist(Integer therapistId, Integer adminUserId) {
        verificarAdmin(adminUserId);
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + therapistId));
        therapist.setApprovalStatus(Therapist.ApprovalStatus.APPROVED);
        return toResponse(therapistRepository.save(therapist));
    }

    @Transactional
    public TherapistDTO.TherapistResponse rejectTherapist(Integer therapistId, Integer adminUserId) {
        verificarAdmin(adminUserId);
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + therapistId));
        therapist.setApprovalStatus(Therapist.ApprovalStatus.REJECTED);
        return toResponse(therapistRepository.save(therapist));
    }

    private void verificarAdmin(Integer adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + adminUserId));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Acceso denegado: se requiere rol ADMIN");
        }
    }
    
    @Transactional(readOnly = true)
    public List<TherapistDTO.TherapistResponse> getTherapistsBySpecialty(String specialty) {
        return therapistRepository.findBySpecialty(specialty).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TherapistDTO.TherapistResponse updateTherapist(Integer id, TherapistDTO.UpdateTherapistRequest request) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + id));
        
        if (request.getBio() != null) {
            therapist.setBio(request.getBio());
        }
        
        if (request.getSpecialty() != null) {
            therapist.setSpecialty(request.getSpecialty());
        }
        
        if (request.getPhotoUrl() != null) {
            therapist.setPhotoUrl(request.getPhotoUrl());
        }
        
        if (request.getPriceAmountCents() != null) {
            therapist.setPriceAmountCents(request.getPriceAmountCents());
        }
        
        if (request.getPriceCurrency() != null) {
            therapist.setPriceCurrency(request.getPriceCurrency());
        }

        if (request.getMpAccessToken() != null && !request.getMpAccessToken().isBlank()) {
            therapist.setMpAccessToken(request.getMpAccessToken().trim());
        }

        Therapist updatedTherapist = therapistRepository.save(therapist);
        return toResponse(updatedTherapist);
    }

    @Transactional
    public void deleteTherapist(Integer id) {
        if (!therapistRepository.existsById(id)) {
            throw new RuntimeException("Terapeuta no encontrado con id: " + id);
        }
        therapistRepository.deleteById(id);
    }

    /**
     * Sube una foto de perfil para el terapeuta.
     * Cualquier usuario autenticado puede subir su propia foto (userId = null omite verificación).
     */
    @Transactional
    public String uploadPhoto(Integer userId, MultipartFile foto) {
        // Verificar que el usuario existe
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        if (foto == null || foto.isEmpty()) {
            throw new RuntimeException("No se recibió ningún archivo");
        }

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Solo se permiten archivos de imagen");
        }

        return storageService.guardarFoto(foto);
    }
}
