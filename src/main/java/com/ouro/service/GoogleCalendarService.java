package com.ouro.service;

import com.ouro.entity.Appointment;
import com.ouro.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Crea/actualiza/borra eventos en el Google Calendar personal de un usuario (terapeuta o
 * cliente), vía REST directo a Calendar API v3. Nunca propaga excepciones: la sincronización
 * con Calendar es best-effort y no debe romper el flujo de reservas.
 */
@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String ART_TIMEZONE = "America/Argentina/Buenos_Aires";
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GoogleCalendarOAuthService oAuthService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public GoogleCalendarService(GoogleCalendarOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public Optional<String> createEvent(User owner, Appointment appointment, boolean isTherapistSide) {
        Optional<String> accessToken = oAuthService.getValidAccessToken(owner);
        if (accessToken.isEmpty()) return Optional.empty();

        try {
            Map<String, Object> body = buildEventBody(appointment, isTherapistSide);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken.get());

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    EVENTS_URL, HttpMethod.POST, new HttpEntity<>(body, headers),
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("id") == null) {
                log.error("Respuesta inválida de Google Calendar al crear evento para userId={}", owner.getId());
                return Optional.empty();
            }
            return Optional.of((String) responseBody.get("id"));
        } catch (Exception e) {
            log.error("Error creando evento de Google Calendar para userId={}: {}", owner.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    public void updateEventTime(User owner, String eventId, LocalDateTime start, LocalDateTime end) {
        if (eventId == null || eventId.isBlank()) return;
        Optional<String> accessToken = oAuthService.getValidAccessToken(owner);
        if (accessToken.isEmpty()) return;

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("start", dateTimeField(start));
            body.put("end", dateTimeField(end));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken.get());

            restTemplate.exchange(
                    EVENTS_URL + "/" + eventId, HttpMethod.PATCH, new HttpEntity<>(body, headers), Void.class
            );
        } catch (Exception e) {
            log.error("Error actualizando evento {} de Google Calendar para userId={}: {}", eventId, owner.getId(), e.getMessage());
        }
    }

    public void deleteEvent(User owner, String eventId) {
        if (eventId == null || eventId.isBlank()) return;
        Optional<String> accessToken = oAuthService.getValidAccessToken(owner);
        if (accessToken.isEmpty()) return;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken.get());
            restTemplate.exchange(
                    EVENTS_URL + "/" + eventId, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class
            );
        } catch (Exception e) {
            // Google devuelve 410 Gone si el evento ya no existe (por ej. borrado a mano) — no es un error real.
            log.warn("No se pudo borrar el evento {} de Google Calendar para userId={}: {}", eventId, owner.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildEventBody(Appointment appointment, boolean isTherapistSide) {
        String otherPartyName = isTherapistSide
                ? appointment.getUser().getFullName()
                : appointment.getTherapist().getUser().getFullName();

        String summary = isTherapistSide
                ? "Sesión con " + otherPartyName
                : "Sesión con " + otherPartyName + " — Ouro";

        StringBuilder description = new StringBuilder();
        if (appointment.getSpecialtyName() != null && !appointment.getSpecialtyName().isBlank()) {
            description.append("Especialidad: ").append(appointment.getSpecialtyName());
        }
        if (isTherapistSide && appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
            if (description.length() > 0) description.append("\n");
            description.append("Notas del cliente: ").append(appointment.getNotes());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("summary", summary);
        body.put("description", description.toString());
        body.put("start", dateTimeField(appointment.getStartAt()));
        body.put("end", dateTimeField(appointment.getEndAt()));
        return body;
    }

    private Map<String, String> dateTimeField(LocalDateTime dateTime) {
        Map<String, String> field = new HashMap<>();
        field.put("dateTime", dateTime.format(ISO_LOCAL));
        field.put("timeZone", ART_TIMEZONE);
        return field;
    }
}
