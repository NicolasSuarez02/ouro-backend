package com.ouro.controller;

import com.ouro.dto.UserDTO;
import com.ouro.entity.User;
import com.ouro.exception.EmailVerificationException;
import com.ouro.security.JwtService;
import com.ouro.service.RateLimiterService;
import com.ouro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;

    @Autowired
    public UserController(UserService userService, JwtService jwtService, RateLimiterService rateLimiterService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@Valid @RequestBody UserDTO.CreateUserRequest request) {
        try {
            UserDTO.UserResponse response = userService.createUser(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Usuario registrado exitosamente. Por favor verifica tu email.");
            result.put("user", response);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        try {
            UserDTO.UserResponse response = userService.verifyEmail(token);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Email verificado exitosamente");
            result.put("user", response);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (EmailVerificationException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestParam String email) {
        try {
            userService.resendVerificationEmail(email);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Email de verificación reenviado exitosamente");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody UserDTO.LoginRequest request,
                                                     HttpServletRequest httpRequest) {
        String clientIp = obtenerIp(httpRequest);
        if (!rateLimiterService.tryConsumeLogin(clientIp)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Demasiados intentos. Esperá un minuto antes de volver a intentar.");
            return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            User user = userService.login(request.getEmail(), request.getPassword());
            String token = jwtService.generarToken(user);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Inicio de sesión exitoso");
            result.put("token", token);
            result.put("user", new UserDTO.UserResponse(user));

            return new ResponseEntity<>(result, HttpStatus.OK);
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
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody UserDTO.ForgotPasswordRequest request) {
        userService.requestPasswordReset(request.getEmail());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Si el email está registrado, recibirás un enlace para restablecer tu contraseña.");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody UserDTO.ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Contraseña restablecida exitosamente");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.UserResponse> getUserById(@PathVariable Integer id) {
        try {
            UserDTO.UserResponse response = userService.getUserById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO.UserResponse> getUserByEmail(@PathVariable String email) {
        try {
            UserDTO.UserResponse response = userService.getUserByEmail(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<UserDTO.UserResponse>> getAllUsers() {
        List<UserDTO.UserResponse> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * GET /api/users/admin?page=0&size=20&search=texto&role=THERAPIST
     * Lista paginada y filtrada de usuarios — solo para ADMIN (verificado por JWT).
     */
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAllUsersPaginados(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Integer adminUserId = currentUserId();
            Map<String, Object> result = userService.getAllUsersPaginados(adminUserId, search, role, page, size);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO.UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserDTO.UpdateUserRequest request) {
        try {
            UserDTO.UserResponse response = userService.updateUser(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /api/users/{id}/admin
     * Elimina un usuario con cascada completa — solo para ADMIN (verificado por JWT).
     */
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<Map<String, Object>> adminDeleteUser(@PathVariable Integer id) {
        try {
            Integer adminUserId = currentUserId();
            userService.adminDeleteUser(id, adminUserId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Usuario eliminado correctamente");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
