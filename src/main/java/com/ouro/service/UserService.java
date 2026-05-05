package com.ouro.service;

import com.ouro.dto.UserDTO;
import com.ouro.entity.Appointment;
import com.ouro.entity.TimeSlot;
import com.ouro.entity.User;
import com.ouro.exception.EmailVerificationException;
import com.ouro.repository.AppointmentRepository;
import com.ouro.repository.AvailabilityRepository;
import com.ouro.repository.RatingRepository;
import com.ouro.repository.ResourceRepository;
import com.ouro.repository.TimeSlotRepository;
import com.ouro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AvailabilityRepository availabilityRepository;
    private final RatingRepository ratingRepository;
    private final ResourceRepository resourceRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AppointmentRepository appointmentRepository,
                       TimeSlotRepository timeSlotRepository,
                       AvailabilityRepository availabilityRepository,
                       RatingRepository ratingRepository,
                       ResourceRepository resourceRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.availabilityRepository = availabilityRepository;
        this.ratingRepository = ratingRepository;
        this.resourceRepository = resourceRepository;
    }
    
    @Transactional
    public UserDTO.UserResponse createUser(UserDTO.CreateUserRequest request) {
        // Verificar si el email ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email ya está registrado");
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        
        // Generar token de verificación
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusHours(24)); // Expira en 24 horas
        user.setEmailVerified(false);
        
        User savedUser = userRepository.save(user);
        log.info("Usuario registrado: id={} email={}", savedUser.getId(), savedUser.getEmail());

        // Enviar email de verificación
        emailService.sendVerificationEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            verificationToken
        );
        
        return new UserDTO.UserResponse(savedUser);
    }
    
    @Transactional
    public User verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new EmailVerificationException("Token de verificación inválido"));

        // Verificar si el token ha expirado
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new EmailVerificationException("El token de verificación ha expirado");
        }

        // Verificar el email
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        User verifiedUser = userRepository.save(user);
        log.info("Email verificado: userId={}", verifiedUser.getId());

        return verifiedUser;
    }
    
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        
        if (user.getEmailVerified()) {
            throw new EmailVerificationException("El email ya está verificado");
        }
        
        // Generar nuevo token
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
        
        userRepository.save(user);
        
        // Enviar nuevo email de verificación
        emailService.sendVerificationEmail(
            user.getEmail(), 
            user.getFullName(), 
            verificationToken
        );
    }
    
    @Transactional(readOnly = true)
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        if (!user.getEmailVerified()) {
            throw new EmailVerificationException("Debes verificar tu email antes de iniciar sesión");
        }

        log.info("Login exitoso: userId={} role={}", user.getId(), user.getRole());
        return user;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        // Siempre retorna sin error aunque el email no exista (evita enumerar usuarios)
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Token de reset inválido o expirado"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new RuntimeException("El token de reset ha expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return new UserDTO.UserResponse(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO.UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        return new UserDTO.UserResponse(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO.UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllUsersPaginados(Integer adminUserId, String search, String role, int page, int size) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Acceso denegado: se requiere rol ADMIN");
        }

        User.Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = User.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Rol inválido: " + role);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> pageResult = userRepository.findAllFiltered(search, roleEnum, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent().stream()
                .map(UserDTO.UserAdminResponse::new)
                .collect(Collectors.toList()));
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("pageNumber", pageResult.getNumber());
        return result;
    }
    
    @Transactional
    public UserDTO.UserResponse updateUser(Integer id, UserDTO.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email ya está registrado");
            }
            user.setEmail(request.getEmail());
            // Si cambia el email, debe re-verificar
            user.setEmailVerified(false);
            
            // Generar nuevo token de verificación
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            user.setVerificationTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
            
            // Enviar email de verificación al nuevo email
            emailService.sendVerificationEmail(
                user.getEmail(), 
                user.getFullName(), 
                verificationToken
            );
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        
        User updatedUser = userRepository.save(user);
        return new UserDTO.UserResponse(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        eliminarUsuarioEnCascada(user);
    }

    @Transactional
    public void adminDeleteUser(Integer targetId, Integer adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Usuario administrador no encontrado"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Acceso denegado: se requiere rol ADMIN");
        }
        if (targetId.equals(adminUserId)) {
            throw new RuntimeException("No podés eliminar tu propia cuenta de administrador");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + targetId));
        eliminarUsuarioEnCascada(target);
    }

    private void eliminarUsuarioEnCascada(User user) {
        // Si es terapeuta: borrar timeslots, turnos (como terapeuta), disponibilidad y ratings recibidos
        if (user.getTherapist() != null) {
            Integer therapistId = user.getTherapist().getId();
            timeSlotRepository.deleteByTherapistId(therapistId);
            appointmentRepository.deleteByTherapistId(therapistId);
            availabilityRepository.deleteByTherapistId(therapistId);
            ratingRepository.deleteAllByTherapistId(therapistId);
        }

        // Liberar timeslots asociados a los turnos donde el usuario es cliente
        List<Appointment> turnosComoCliente = appointmentRepository.findByUserId(user.getId());
        for (Appointment appt : turnosComoCliente) {
            timeSlotRepository.findByAppointmentId(appt.getId()).ifPresent(slot -> {
                slot.setStatus(TimeSlot.SlotStatus.FREE);
                slot.setAppointment(null);
                timeSlotRepository.save(slot);
            });
        }
        appointmentRepository.deleteByUserId(user.getId());

        // Borrar ratings que este usuario dio
        ratingRepository.deleteAllByUserId(user.getId());

        // Borrar recursos subidos por este usuario
        resourceRepository.deleteByUploadedById(user.getId());

        userRepository.deleteById(user.getId());
        log.info("Usuario eliminado: id={} email={}", user.getId(), user.getEmail());
    }
}
