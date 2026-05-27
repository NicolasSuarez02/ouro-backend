package com.ouro.controller;

import com.ouro.entity.Therapist;
import com.ouro.repository.TherapistRepository;
import com.ouro.service.AppointmentService;
import com.ouro.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")

public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Value("${mercadopago.client-secret}")
    private String mpClientSecret;

    private final PaymentService paymentService;
    private final AppointmentService appointmentService;
    private final TherapistRepository therapistRepository;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             AppointmentService appointmentService,
                             TherapistRepository therapistRepository) {
        this.paymentService = paymentService;
        this.appointmentService = appointmentService;
        this.therapistRepository = therapistRepository;
    }


    /**
     * Webhook de Mercado Pago con therapistId en la URL.
     * Permite usar el access token propio del terapeuta para consultar el pago.
     */
    @PostMapping("/webhook/{therapistId}")
    public ResponseEntity<Void> receiveTherapistWebhook(
            @PathVariable Integer therapistId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        try {
            String type = (String) body.get("type");
            log.info("Webhook MP recibido. therapistId={} type={}", therapistId, type);

            if ("payment".equals(type)) {
                Long paymentId = paymentService.extractPaymentIdFromWebhook(body);
                if (paymentId != null) {
                    verifyWebhookSignature(httpRequest, paymentId.toString());
                    String therapistToken = therapistRepository.findById(therapistId)
                            .map(Therapist::getMpAccessToken)
                            .orElse(null);

                    String externalRef = paymentService.getExternalReferenceIfApproved(paymentId, therapistToken);
                    if (externalRef != null) {
                        Integer appointmentId = Integer.parseInt(externalRef);
                        appointmentService.confirmPayment(appointmentId);
                        log.info("Turno {} confirmado tras pago MP {} (terapeuta {})", appointmentId, paymentId, therapistId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook MP (terapeuta {}): {}", therapistId, e.getMessage());
        }
        // Siempre retornar 200 para que MP no reintente
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook genérico (sin therapistId). Usa el token global de la plataforma.
     * Mantenido por compatibilidad con turnos creados antes de la integración por terapeuta.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        try {
            String type = (String) body.get("type");
            log.info("Webhook MP genérico recibido. type={}", type);

            if ("payment".equals(type)) {
                Long paymentId = paymentService.extractPaymentIdFromWebhook(body);
                if (paymentId != null) {
                    verifyWebhookSignature(httpRequest, paymentId.toString());
                    String externalRef = paymentService.getExternalReferenceIfApproved(paymentId, null);
                    if (externalRef != null) {
                        Integer appointmentId = Integer.parseInt(externalRef);
                        appointmentService.confirmPayment(appointmentId);
                        log.info("Turno {} confirmado tras pago MP {} (webhook genérico)", appointmentId, paymentId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook MP genérico: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simular-pago")
    public ResponseEntity<Void> simulatePayment(@RequestParam Integer appointmentId) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            appointmentService.confirmPayment(appointmentId);
            log.info("Pago simulado para turno {}", appointmentId);
        } catch (Exception e) {
            log.error("Error al simular pago para turno {}: {}", appointmentId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/webhook")
    public ResponseEntity<Void> webhookPing() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/webhook/{therapistId}")
    public ResponseEntity<Void> webhookPingTherapist(@PathVariable Integer therapistId) {
        return ResponseEntity.ok().build();
    }

    private void verifyWebhookSignature(HttpServletRequest request, String dataId) {
        String xSig = request.getHeader("x-signature");
        String xRequestId = request.getHeader("x-request-id");
        if (xSig == null) {
            log.debug("Webhook sin x-signature");
            return;
        }
        String ts = null, v1 = null;
        for (String part : xSig.split(",")) {
            String p = part.trim();
            if (p.startsWith("ts=")) ts = p.substring(3);
            else if (p.startsWith("v1=")) v1 = p.substring(3);
        }
        if (ts == null || v1 == null) {
            log.warn("Webhook MP: x-signature mal formado: {}", xSig);
            return;
        }
        String manifest = "id:" + dataId + ";request-id:" + (xRequestId != null ? xRequestId : "") + ";ts:" + ts;
        String computed = hmacSha256(mpClientSecret, manifest);
        if (!computed.equals(v1)) {
            log.warn("Webhook MP: firma no coincide para dataId={} — procesando de todas formas", dataId);
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
            throw new RuntimeException("Error calculando HMAC", e);
        }
    }

}
