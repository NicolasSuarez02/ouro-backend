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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MercadoPagoOAuthService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoOAuthService.class);
    private static final String MP_AUTH_URL = "https://auth.mercadopago.com/authorization";
    private static final String MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";

    @Value("${mercadopago.client-id}")
    private String clientId;

    @Value("${mercadopago.client-secret}")
    private String clientSecret;

    @Value("${app.backend.url}")
    private String backendUrl;

    private final TherapistRepository therapistRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public MercadoPagoOAuthService(TherapistRepository therapistRepository) {
        this.therapistRepository = therapistRepository;
    }

    public String generarUrlAutorizacion(Integer userId) {
        String redirectUri = URLEncoder.encode(
                backendUrl + "/api/therapists/mp-callback",
                StandardCharsets.UTF_8
        );
        return MP_AUTH_URL
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&platform_id=mp"
                + "&redirect_uri=" + redirectUri
                + "&state=" + userId;
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
