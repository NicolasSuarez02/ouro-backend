package com.ouro.service;

import com.ouro.entity.User;
import com.ouro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth de Google Calendar, uno por usuario (terapeuta o cliente), ambos opcionales.
 * Mismo patrón que MercadoPagoOAuthService.
 */
@Service
public class GoogleCalendarOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarOAuthService.class);
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final long STATE_TTL_SECONDS = 600;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public GoogleCalendarOAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateAuthorizationUrl(Integer userId) {
        long ts = Instant.now().getEpochSecond();
        String payload = userId + "." + ts;
        String signature = hmacSha256(jwtSecret, payload);
        String state = payload + "." + signature;

        String redirectUri = URLEncoder.encode(
                backendUrl + "/api/google-calendar/callback",
                StandardCharsets.UTF_8
        );
        return GOOGLE_AUTH_URL
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&scope=" + URLEncoder.encode(CALENDAR_SCOPE, StandardCharsets.UTF_8)
                + "&redirect_uri=" + redirectUri
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    public Integer extractUserIdFromState(String state) {
        String[] parts = state.split("\\.", 3);
        if (parts.length != 3) {
            throw new RuntimeException("State OAuth inválido");
        }
        try {
            long ts = Long.parseLong(parts[1]);
            if (Instant.now().getEpochSecond() - ts > STATE_TTL_SECONDS) {
                throw new RuntimeException("State OAuth expirado");
            }
            String expected = hmacSha256(jwtSecret, parts[0] + "." + parts[1]);
            if (!expected.equals(parts[2])) {
                throw new RuntimeException("Firma inválida en state OAuth");
            }
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("State OAuth mal formado");
        }
    }

    private String hmacSha256(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generando HMAC", e);
        }
    }

    @Transactional
    public void processCallback(String code, Integer userId) {
        String redirectUri = backendUrl + "/api/google-calendar/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                GOOGLE_TOKEN_URL, HttpMethod.POST, request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new RuntimeException("Respuesta inválida de Google al obtener el token");
        }

        String accessToken = (String) responseBody.get("access_token");
        String refreshToken = (String) responseBody.get("refresh_token");
        Number expiresIn = (Number) responseBody.get("expires_in");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para userId: " + userId));

        user.setGoogleAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            // Google solo manda refresh_token la primera vez que el usuario da consentimiento.
            // Si ya había uno guardado, se conserva.
            user.setGoogleRefreshToken(refreshToken);
        }
        user.setGoogleTokenExpiry(LocalDateTime.now(ZoneOffset.UTC)
                .plusSeconds(expiresIn != null ? expiresIn.longValue() : 3600));

        userRepository.save(user);
        log.info("Token Google Calendar guardado para userId={}", user.getId());
    }

    /**
     * Devuelve un access_token válido, refrescándolo si venció. Si el usuario nunca conectó
     * Google o el refresh falla (token revocado), limpia la conexión y devuelve vacío.
     */
    @Transactional
    public Optional<String> getValidAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            return Optional.empty();
        }

        boolean expired = user.getGoogleTokenExpiry() == null
                || !user.getGoogleTokenExpiry().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));

        if (!expired) {
            return Optional.ofNullable(user.getGoogleAccessToken());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", user.getGoogleRefreshToken());

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, headers),
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Respuesta inválida de Google al refrescar el token");
            }

            String accessToken = (String) responseBody.get("access_token");
            Number expiresIn = (Number) responseBody.get("expires_in");

            user.setGoogleAccessToken(accessToken);
            user.setGoogleTokenExpiry(LocalDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(expiresIn != null ? expiresIn.longValue() : 3600));
            userRepository.save(user);

            return Optional.of(accessToken);
        } catch (RestClientException e) {
            log.error("No se pudo refrescar el token de Google Calendar para userId={}: {}", user.getId(), e.getMessage());
            user.setGoogleAccessToken(null);
            user.setGoogleRefreshToken(null);
            user.setGoogleTokenExpiry(null);
            userRepository.save(user);
            return Optional.empty();
        }
    }

    @Transactional
    public void disconnect(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleTokenExpiry(null);
        userRepository.save(user);
    }
}
