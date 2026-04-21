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
    private static final String ZOOM_MEETINGS_URL = "https://api.zoom.us/v2/users/me/meetings";
    private static final String TIMEZONE = "America/Argentina/Buenos_Aires";

    @Value("${zoom.account-id}")
    private String accountId;

    @Value("${zoom.client-id}")
    private String clientId;

    @Value("${zoom.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private String obtenerAccessToken() {
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
     * Crea un meeting de Zoom para el turno dado.
     * Retorna el join_url del meeting, o null si falla.
     */
    public String crearMeeting(Appointment appointment) {
        try {
            String accessToken = obtenerAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String startTime = appointment.getStartAt()
                    .atZone(ZoneId.of(TIMEZONE))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            long duracionMinutos = java.time.Duration
                    .between(appointment.getStartAt(), appointment.getEndAt())
                    .toMinutes();

            Map<String, Object> settings = new HashMap<>();
            settings.put("join_before_host", true);
            settings.put("waiting_room", false);
            settings.put("meeting_authentication", false);

            Map<String, Object> meetingBody = new HashMap<>();
            meetingBody.put("topic", "Sesión con " + appointment.getTherapist().getUser().getFullName());
            meetingBody.put("type", 2); // meeting programado
            meetingBody.put("start_time", startTime);
            meetingBody.put("duration", duracionMinutos > 0 ? duracionMinutos : 60);
            meetingBody.put("timezone", TIMEZONE);
            meetingBody.put("settings", settings);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(meetingBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ZOOM_MEETINGS_URL, request, Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null) {
                String joinUrl = (String) responseBody.get("join_url");
                log.info("Meeting de Zoom creado para turno {}: {}", appointment.getId(), joinUrl);
                return joinUrl;
            }
        } catch (Exception e) {
            log.error("Error al crear meeting de Zoom para turno {}: {}", appointment.getId(), e.getMessage());
        }
        return null;
    }
}
