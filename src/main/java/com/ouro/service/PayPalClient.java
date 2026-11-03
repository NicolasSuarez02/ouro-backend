package com.ouro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Llamadas HTTP directas a la REST API de PayPal (sin SDK, misma regla que con Mercado Pago).
 * Cobra con las credenciales de la app de Ouro; el dinero va al payee de cada orden.
 */
@Service
public class PayPalClient {

    private static final Logger log = LoggerFactory.getLogger(PayPalClient.class);

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.webhook-id:}")
    private String webhookId;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String cachedToken;
    private Instant cachedTokenExpiresAt = Instant.EPOCH;

    /** Resultado de una captura. status: COMPLETED, PENDING, DECLINED o FAILED. */
    public record CaptureResult(String status, String captureId, String amountValue, String currency, String detail) {}

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }
        try {
            String basic = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/oauth2/token"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new RuntimeException("PayPal token HTTP " + res.statusCode() + ": " + res.body());
            }
            JsonNode root = mapper.readTree(res.body());
            cachedToken = root.path("access_token").asText();
            long expiresIn = root.path("expires_in").asLong(3000);
            // Renovar 5 minutos antes de que venza
            cachedTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
            return cachedToken;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener el token de PayPal: " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> send(String method, String path, String jsonBody, String requestId) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", "application/json");
        if (requestId != null) {
            // Idempotencia: si se reintenta con el mismo id, PayPal no duplica la operación
            b.header("PayPal-Request-Id", requestId);
        }
        HttpRequest.BodyPublisher body = jsonBody != null
                ? HttpRequest.BodyPublishers.ofString(jsonBody)
                : HttpRequest.BodyPublishers.noBody();
        b.method(method, body);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Crea una orden de cobro en USD con la terapeuta como payee. Devuelve [orderId, approveUrl].
     */
    public String[] createOrder(Integer checkoutId, String payeeEmail, int amountUsdCents,
                                String description, String returnUrl, String cancelUrl) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("intent", "CAPTURE");
            ObjectNode unit = root.putArray("purchase_units").addObject();
            unit.put("reference_id", "checkout-" + checkoutId);
            unit.put("custom_id", "checkout-" + checkoutId);
            unit.put("description", truncate(description, 127));
            ObjectNode amount = unit.putObject("amount");
            amount.put("currency_code", "USD");
            amount.put("value", formatUsd(amountUsdCents));
            unit.putObject("payee").put("email_address", payeeEmail);
            ObjectNode ctx = root.putObject("payment_source").putObject("paypal").putObject("experience_context");
            ctx.put("brand_name", "Ouro");
            ctx.put("shipping_preference", "NO_SHIPPING");
            ctx.put("user_action", "PAY_NOW");
            ctx.put("locale", "es-AR");
            ctx.put("return_url", returnUrl);
            ctx.put("cancel_url", cancelUrl);

            HttpResponse<String> res = send("POST", "/v2/checkout/orders",
                    mapper.writeValueAsString(root), "ouro-checkout-" + checkoutId);
            if (res.statusCode() != 200 && res.statusCode() != 201) {
                log.error("PayPal createOrder HTTP {}: {}", res.statusCode(), res.body());
                throw new RuntimeException("PayPal rechazó la orden");
            }
            JsonNode order = mapper.readTree(res.body());
            String orderId = order.path("id").asText();
            String approveUrl = null;
            for (JsonNode link : order.path("links")) {
                String rel = link.path("rel").asText();
                if ("payer-action".equals(rel) || "approve".equals(rel)) {
                    approveUrl = link.path("href").asText();
                }
            }
            if (orderId.isBlank() || approveUrl == null) {
                throw new RuntimeException("Respuesta de PayPal sin id o link de pago");
            }
            return new String[]{orderId, approveUrl};
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error creando la orden en PayPal: " + e.getMessage(), e);
        }
    }

    /** Captura (cobra) una orden aprobada por el cliente. */
    public CaptureResult captureOrder(String orderId) {
        try {
            HttpResponse<String> res = send("POST", "/v2/checkout/orders/" + orderId + "/capture",
                    "{}", "ouro-capture-" + orderId);
            JsonNode root = mapper.readTree(res.body().isBlank() ? "{}" : res.body());
            if (res.statusCode() == 200 || res.statusCode() == 201) {
                return parseCapture(root);
            }
            String issue = root.path("details").path(0).path("issue").asText("");
            if ("ORDER_ALREADY_CAPTURED".equals(issue)) {
                // Ya se cobró antes (por ejemplo un reintento): leer la captura existente
                return parseCapture(getOrder(orderId));
            }
            log.warn("PayPal capture {} HTTP {}: {}", orderId, res.statusCode(), res.body());
            if ("INSTRUMENT_DECLINED".equals(issue)) {
                return new CaptureResult("DECLINED", null, null, null, issue);
            }
            // ORDER_NOT_APPROVED / PAYER_ACTION_REQUIRED: el cliente todavía no aprobó.
            // Se trata como reintentable para no dar por perdida una orden que puede aprobarse después.
            return new CaptureResult("FAILED", null, null, null, issue.isBlank() ? "HTTP " + res.statusCode() : issue);
        } catch (Exception e) {
            log.error("Error capturando orden PayPal {}: {}", orderId, e.getMessage());
            return new CaptureResult("FAILED", null, null, null, e.getMessage());
        }
    }

    public JsonNode getOrder(String orderId) {
        try {
            HttpResponse<String> res = send("GET", "/v2/checkout/orders/" + orderId, null, null);
            if (res.statusCode() != 200) {
                throw new RuntimeException("PayPal getOrder HTTP " + res.statusCode() + ": " + res.body());
            }
            return mapper.readTree(res.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error consultando la orden en PayPal: " + e.getMessage(), e);
        }
    }

    private CaptureResult parseCapture(JsonNode order) {
        JsonNode capture = order.path("purchase_units").path(0).path("payments").path("captures").path(0);
        if (capture.isMissingNode()) {
            return new CaptureResult("FAILED", null, null, null, "orden sin captura, status " + order.path("status").asText());
        }
        String status = capture.path("status").asText();
        String normalized = switch (status) {
            case "COMPLETED" -> "COMPLETED";
            case "PENDING" -> "PENDING";
            case "DECLINED", "FAILED" -> "DECLINED";
            default -> "FAILED";
        };
        return new CaptureResult(normalized,
                capture.path("id").asText(null),
                capture.path("amount").path("value").asText(null),
                capture.path("amount").path("currency_code").asText(null),
                status);
    }

    /**
     * Valida la firma de un webhook con la API de PayPal. El evento se reenvía tal cual llegó
     * (sin volver a serializarlo) para que la firma coincida.
     */
    public boolean verifyWebhookSignature(String authAlgo, String certUrl, String transmissionId,
                                          String transmissionSig, String transmissionTime, String rawEvent) {
        if (webhookId == null || webhookId.isBlank()) {
            log.error("paypal.webhook-id no configurado: se rechazan todos los webhooks de PayPal");
            return false;
        }
        if (authAlgo == null || certUrl == null || transmissionId == null
                || transmissionSig == null || transmissionTime == null) {
            return false;
        }
        try {
            String body = "{"
                    + "\"auth_algo\":" + mapper.writeValueAsString(authAlgo) + ","
                    + "\"cert_url\":" + mapper.writeValueAsString(certUrl) + ","
                    + "\"transmission_id\":" + mapper.writeValueAsString(transmissionId) + ","
                    + "\"transmission_sig\":" + mapper.writeValueAsString(transmissionSig) + ","
                    + "\"transmission_time\":" + mapper.writeValueAsString(transmissionTime) + ","
                    + "\"webhook_id\":" + mapper.writeValueAsString(webhookId) + ","
                    + "\"webhook_event\":" + rawEvent
                    + "}";
            HttpResponse<String> res = send("POST", "/v1/notifications/verify-webhook-signature", body, null);
            if (res.statusCode() != 200) {
                log.warn("PayPal verify-webhook-signature HTTP {}: {}", res.statusCode(), res.body());
                return false;
            }
            return "SUCCESS".equals(mapper.readTree(res.body()).path("verification_status").asText());
        } catch (Exception e) {
            log.error("Error verificando firma de webhook PayPal: {}", e.getMessage());
            return false;
        }
    }

    public static String formatUsd(int cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    /** "45.00" → 4500. Devuelve null si no se puede leer. */
    public static Integer parseUsdCents(String value) {
        if (value == null) return null;
        try {
            return new java.math.BigDecimal(value).movePointRight(2).intValueExact();
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
