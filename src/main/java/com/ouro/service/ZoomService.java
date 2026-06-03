package com.ouro.service;

import com.ouro.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ZoomService {

    private static final Logger log = LoggerFactory.getLogger(ZoomService.class);
    private static final String ZOOM_TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String ZOOM_USERS_URL = "https://api.zoom.us/v2/users";
    private static final String TIMEZONE = "America/Argentina/Buenos_Aires";

    @Value("${zoom.account-id}")
    private String accountId;

    @Value("${zoom.client-id}")
    private String clientId;

    @Value("${zoom.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getAccessToken() {
        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "account_credentials");
        body.add("account_id", accountId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(ZOOM_TOKEN_URL, request, Map.class);

        return (String) Objects.requireNonNull(response.getBody()).get("access_token");
    }

    /**
     * Crea un usuario en la cuenta Zoom de la plataforma (custCreate = sin email de invitación).
     * Retorna el Zoom userId, o null si falla.
     */
    public String createZoomUser(String email, String firstName, String lastName) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", email);
            userInfo.put("type", 1);
            userInfo.put("first_name", firstName);
            userInfo.put("last_name", lastName != null ? lastName : "");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("action", "create");
            requestBody.put("user_info", userInfo);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ZOOM_USERS_URL, request, Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null) {
                String zoomUserId = (String) responseBody.get("id");
                log.info("Usuario Zoom creado para {}: {}", email, zoomUserId);
                return zoomUserId;
            }
        } catch (Exception e) {
            log.error("Error al crear usuario Zoom para {}: {}", email, e.getMessage());
        }
        return null;
    }

    public record ZoomMeetingUrls(String meetingId, String joinUrl, String startUrl) {}

    /**
     * Crea un meeting de Zoom para el turno dado, bajo la cuenta del terapeuta.
     * Retorna meetingId + ambas URLs (join para el cliente, start para el terapeuta/host), o null si falla.
     */
    public ZoomMeetingUrls createMeeting(Appointment appointment) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String startTime = appointment.getStartAt()
                    .atZone(ZoneId.of(TIMEZONE))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            long durationMinutes = java.time.Duration
                    .between(appointment.getStartAt(), appointment.getEndAt())
                    .toMinutes();

            Map<String, Object> settings = new HashMap<>();
            settings.put("join_before_host", false);
            settings.put("waiting_room", false);
            settings.put("meeting_authentication", false);

            Map<String, Object> meetingBody = new HashMap<>();
            meetingBody.put("topic", "Sesión con " + appointment.getTherapist().getUser().getFullName());
            meetingBody.put("type", 2);
            meetingBody.put("start_time", startTime);
            meetingBody.put("duration", durationMinutes > 0 ? durationMinutes : 60);
            meetingBody.put("timezone", TIMEZONE);
            meetingBody.put("settings", settings);

            String zoomUserId = appointment.getTherapist().getZoomUserId();
            String meetingsUrl = (zoomUserId != null && !zoomUserId.isBlank())
                    ? ZOOM_USERS_URL + "/" + zoomUserId + "/meetings"
                    : ZOOM_USERS_URL + "/me/meetings";

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(meetingBody, headers);
            ResponseEntity<Map> response;
            try {
                response = restTemplate.postForEntity(meetingsUrl, request, Map.class);
            } catch (Exception ex) {
                if (zoomUserId != null && !zoomUserId.isBlank()) {
                    log.warn("Falló meeting bajo userId {}, reintentando bajo me: {}", zoomUserId, ex.getMessage());
                    response = restTemplate.postForEntity(ZOOM_USERS_URL + "/me/meetings", request, Map.class);
                } else {
                    throw ex;
                }
            }

            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null) {
                String meetingId = String.valueOf(responseBody.get("id"));
                String joinUrl = (String) responseBody.get("join_url");
                String startUrl = (String) responseBody.get("start_url");
                log.info("Meeting de Zoom creado para turno {}: meetingId={}, join={}", appointment.getId(), meetingId, joinUrl);
                return new ZoomMeetingUrls(meetingId, joinUrl, startUrl);
            }
        } catch (Exception e) {
            log.error("Error al crear meeting de Zoom para turno {}: {}", appointment.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Elimina un meeting de Zoom. Silencia errores (ej. meeting ya no existe en Zoom).
     */
    public void deleteMeeting(String meetingId) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(
                    "https://api.zoom.us/v2/meetings/" + meetingId,
                    org.springframework.http.HttpMethod.DELETE,
                    request,
                    Void.class
            );
            log.info("Meeting de Zoom {} eliminado", meetingId);
        } catch (Exception e) {
            log.warn("No se pudo eliminar meeting de Zoom {}: {}", meetingId, e.getMessage());
        }
    }
}
