package com.ouro.service;

import com.ouro.entity.Therapist;
import com.ouro.repository.TherapistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
public class MercadoPagoOAuthService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoOAuthService.class);
    private static final String MP_AUTH_URL = "https://auth.mercadopago.com/authorization";
    private static final String MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";
    private static final long STATE_TTL_SECONDS = 600;

    @Value("${mercadopago.client-id}")
    private String clientId;

    @Value("${mercadopago.client-secret}")
    private String clientSecret;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final TherapistRepository therapistRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public MercadoPagoOAuthService(TherapistRepository therapistRepository) {
        this.therapistRepository = therapistRepository;
    }

    public String generarUrlAutorizacion(Integer userId) {
        long ts = Instant.now().getEpochSecond();
        String payload = userId + "." + ts;
        String signature = hmacSha256(jwtSecret, payload);
        String state = payload + "." + signature;

        String redirectUri = URLEncoder.encode(
                backendUrl + "/api/therapists/mp-callback",
                StandardCharsets.UTF_8
        );
        return MP_AUTH_URL
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&platform_id=mp"
                + "&redirect_uri=" + redirectUri
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    public Integer extraerUserIdDeState(String state) {
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
    public void procesarCallback(String code, Integer userId) {
        String redirectUri = backendUrl + "/api/therapists/mp-callback";

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
                MP_TOKEN_URL, HttpMethod.POST, request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new RuntimeException("Respuesta inválida de Mercado Pago al obtener el token");
        }

        String accessToken = (String) responseBody.get("access_token");

        Therapist therapist = therapistRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado para userId: " + userId));
        therapist.setMpAccessToken(accessToken);
        therapistRepository.save(therapist);
        log.info("Token MP OAuth guardado para therapistId={}", therapist.getId());
    }
}
