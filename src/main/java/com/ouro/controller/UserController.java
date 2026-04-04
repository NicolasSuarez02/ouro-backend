package com.ouro.controller;

import com.ouro.dto.UserDTO;
import com.ouro.exception.EmailVerificationException;
import com.ouro.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody UserDTO.LoginRequest request) {
        try {
            UserDTO.UserResponse response = userService.login(request.getEmail(), request.getPassword());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Inicio de sesión exitoso");
            result.put("user", response);

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
}
