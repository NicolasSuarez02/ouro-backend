package com.ouro.controller;

import com.ouro.service.GoogleCalendarOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Conexión OAuth de Google Calendar. La usan tanto terapeutas como clientes, cada uno de forma
 * independiente y opcional (mismo patrón que /api/therapists/mp-connect y mp-callback).
 */
@RestController
@RequestMapping("/api/google-calendar")
public class GoogleCalendarController {

    private final GoogleCalendarOAuthService googleCalendarOAuthService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public GoogleCalendarController(GoogleCalendarOAuthService googleCalendarOAuthService) {
        this.googleCalendarOAuthService = googleCalendarOAuthService;
    }

    /**
     * GET /api/google-calendar/connect — retorna la URL de autorización de Google OAuth.
     * Requiere JWT (el userId se extrae del token).
     */
    @GetMapping("/connect")
    public ResponseEntity<Object> getConnectUrl() {
        try {
            Integer userId = currentUserId();
            String authUrl = googleCalendarOAuthService.generateAuthorizationUrl(userId);
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
     * GET /api/google-calendar/callback — callback de Google OAuth.
     * Google redirige aquí con code + state (userId). Guarda el token y redirige al frontend.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        String baseUrl = frontendUrl.trim();
        if (!baseUrl.startsWith("http")) baseUrl = "https://" + baseUrl;
        try {
            Integer userId = googleCalendarOAuthService.extractUserIdFromState(state);
            googleCalendarOAuthService.processCallback(code, userId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", baseUrl + "/dashboard?google=success")
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", baseUrl + "/dashboard?google=error")
                    .build();
        }
    }

    /**
     * POST /api/google-calendar/disconnect — desconecta Google Calendar. Requiere JWT.
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        try {
            googleCalendarOAuthService.disconnect(currentUserId());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
