package com.ouro.service;

import com.ouro.dto.TherapistDTO;
import com.ouro.entity.Therapist;
import com.ouro.entity.TherapistSpecialty;
import com.ouro.entity.User;
import com.ouro.exception.EmailVerificationException;
import com.ouro.repository.RatingRepository;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.TherapistSpecialtyRepository;
import com.ouro.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TherapistService {

    private static final Logger log = LoggerFactory.getLogger(TherapistService.class);

    private final TherapistRepository therapistRepository;
    private final TherapistSpecialtyRepository specialtyRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final RatingRepository ratingRepository;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Autowired
    public TherapistService(TherapistRepository therapistRepository,
                            TherapistSpecialtyRepository specialtyRepository,
                            UserRepository userRepository,
                            StorageService storageService,
                            RatingRepository ratingRepository) {
        this.therapistRepository = therapistRepository;
        this.specialtyRepository = specialtyRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.ratingRepository = ratingRepository;
    }

    @PostConstruct
    public void generarSlugsFaltantes() {
        try {
            therapistRepository.findAll().forEach(t -> {
                if (t.getSlug() == null || t.getSlug().isBlank()) {
                    String nombre = t.getUser() != null ? t.getUser().getFullName() : null;
                    String slug = generarSlugUnico(nombre, t.getId());
                    if (slug != null) {
                        t.setSlug(slug);
                        therapistRepository.save(t);
                        log.info("Slug generado para terapeuta {}: {}", t.getId(), slug);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("No se pudieron generar slugs (¿falta la columna slug en la BD?): {}", e.getMessage());
        }
    }

    private TherapistDTO.TherapistResponse toResponse(Therapist therapist) {
        Double avg = ratingRepository.findAverageScoreByTherapistId(therapist.getId());
        long count = ratingRepository.countByTherapistId(therapist.getId());
        return new TherapistDTO.TherapistResponse(therapist, avg, count);
    }

    private String generarSlug(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        String normalized = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        return normalized
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String generarSlugUnico(String nombre, Integer therapistId) {
        String base = generarSlug(nombre);
        if (base == null) return null;
        String candidate = base;
        int i = 2;
        while (true) {
            String finalCandidate = candidate;
            boolean taken = therapistRepository.findBySlug(finalCandidate)
                    .map(t -> !t.getId().equals(therapistId))
                    .orElse(false);
            if (!taken) return candidate;
            candidate = base + "-" + i++;
        }
    }

    @Transactional
    public TherapistDTO.TherapistResponse createTherapist(TherapistDTO.CreateTherapistRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + request.getUserId()));

        if (!user.getEmailVerified()) {
            throw new EmailVerificationException("Debe verificar su email antes de crear un perfil de terapeuta");
        }

        if (therapistRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("El usuario ya tiene un perfil de terapeuta");
        }

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
        if (request.getMinBookingLeadHours() != null) {
            therapist.setMinBookingLeadHours(request.getMinBookingLeadHours());
        }

        Therapist saved = therapistRepository.save(therapist);

        String slug = generarSlugUnico(user.getFullName(), saved.getId());
        saved.setSlug(slug);

        if (request.getSpecialties() != null && !request.getSpecialties().isEmpty()) {
            for (TherapistDTO.SpecialtyDTO sd : request.getSpecialties()) {
                TherapistSpecialty sp = new TherapistSpecialty();
                sp.setTherapist(saved);
                sp.setName(sd.getName());
                sp.setMinBookingLeadHours(sd.getMinBookingLeadHours() != null ? sd.getMinBookingLeadHours() : 1);
                saved.getSpecialties().add(sp);
            }
        }

        return toResponse(therapistRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public TherapistDTO.TherapistResponse getTherapistById(Integer id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + id));
        return toResponse(therapist);
    }

    @Transactional(readOnly = true)
    public TherapistDTO.TherapistResponse getTherapistBySlug(String slug) {
        Therapist therapist;
        // Si es numérico, buscar por ID para mantener compatibilidad con URLs antiguas
        try {
            int id = Integer.parseInt(slug);
            therapist = therapistRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));
        } catch (NumberFormatException e) {
            therapist = therapistRepository.findBySlug(slug)
                    .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con slug: " + slug));
        }
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
    public TherapistDTO.TherapistResponse updateTherapist(Integer id, TherapistDTO.UpdateTherapistRequest request, Integer requestingUserId) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + id));
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        boolean isAdmin = requestingUser.getRole() == User.Role.ADMIN;
        boolean isOwner = therapist.getUser().getId().equals(requestingUserId);
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Acceso denegado: solo el propio terapeuta o un administrador puede editar este perfil");
        }

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
        if (request.getMinBookingLeadHours() != null) {
            therapist.setMinBookingLeadHours(request.getMinBookingLeadHours());
        }

        // Gestión de especialidades: reemplaza todas si viene la lista
        if (request.getSpecialties() != null) {
            therapist.getSpecialties().clear();
            for (TherapistDTO.SpecialtyDTO sd : request.getSpecialties()) {
                TherapistSpecialty sp = new TherapistSpecialty();
                sp.setTherapist(therapist);
                sp.setName(sd.getName());
                sp.setMinBookingLeadHours(sd.getMinBookingLeadHours() != null ? sd.getMinBookingLeadHours() : 1);
                therapist.getSpecialties().add(sp);
            }
        }

        return toResponse(therapistRepository.save(therapist));
    }

    @Transactional
    public void deleteTherapist(Integer id, Integer requestingUserId) {
        verificarAdmin(requestingUserId);
        if (!therapistRepository.existsById(id)) {
            throw new RuntimeException("Terapeuta no encontrado con id: " + id);
        }
        therapistRepository.deleteById(id);
    }

    @Transactional
    public String uploadPhoto(Integer userId, MultipartFile foto) {
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
