package com.ouro.service;

import com.ouro.dto.UserDTO;
import com.ouro.entity.Therapist;
import com.ouro.entity.User;
import com.ouro.exception.EmailVerificationException;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // Expira en 24 horas
        user.setEmailVerified(false);
        
        User savedUser = userRepository.save(user);
        
        // Enviar email de verificación
        emailService.sendVerificationEmail(
            savedUser.getEmail(), 
            savedUser.getFullName(), 
            verificationToken
        );
        
        return new UserDTO.UserResponse(savedUser);
    }
    
    @Transactional
    public UserDTO.UserResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new EmailVerificationException("Token de verificación inválido"));
        
        // Verificar si el token ha expirado
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new EmailVerificationException("El token de verificación ha expirado");
        }
        
        // Verificar el email
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        
        User verifiedUser = userRepository.save(user);
        
        return new UserDTO.UserResponse(verifiedUser);
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
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        
        userRepository.save(user);
        
        // Enviar nuevo email de verificación
        emailService.sendVerificationEmail(
            user.getEmail(), 
            user.getFullName(), 
            verificationToken
        );
    }
    
    @Transactional(readOnly = true)
    public UserDTO.UserResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        if (!user.getEmailVerified()) {
            throw new EmailVerificationException("Debes verificar tu email antes de iniciar sesión");
        }

        if (user.getTherapist() != null &&
                user.getTherapist().getApprovalStatus() != Therapist.ApprovalStatus.APPROVED) {
            throw new RuntimeException("Tu perfil de terapeuta está pendiente de aprobación");
        }

        return new UserDTO.UserResponse(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        // Siempre retorna sin error aunque el email no exista (evita enumerar usuarios)
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Token de reset inválido o expirado"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
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
            user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
            
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
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con id: " + id);
        }
        userRepository.deleteById(id);
    }
}
