package com.ouro.controller;

import com.ouro.entity.Therapist;
import com.ouro.repository.TherapistRepository;
import com.ouro.service.AppointmentService;
import com.ouro.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

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
    public ResponseEntity<Void> recibirWebhookTerapeuta(
            @PathVariable Integer therapistId,
            @RequestBody Map<String, Object> body) {
        try {
            String type = (String) body.get("type");
            log.info("Webhook MP recibido. therapistId={} type={}", therapistId, type);

            if ("payment".equals(type)) {
                Long paymentId = paymentService.extraerPagoIdDeWebhook(body);
                if (paymentId != null) {
                    String tokenTerapeuta = therapistRepository.findById(therapistId)
                            .map(Therapist::getMpAccessToken)
                            .orElse(null);

                    String externalRef = paymentService.obtenerExternalReferenceDeAprobado(paymentId, tokenTerapeuta);
                    if (externalRef != null) {
                        Integer appointmentId = Integer.parseInt(externalRef);
                        appointmentService.confirmarPago(appointmentId);
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
    public ResponseEntity<Void> recibirWebhook(@RequestBody Map<String, Object> body) {
        try {
            String type = (String) body.get("type");
            log.info("Webhook MP genérico recibido. type={}", type);

            if ("payment".equals(type)) {
                Long paymentId = paymentService.extraerPagoIdDeWebhook(body);
                if (paymentId != null) {
                    String externalRef = paymentService.obtenerExternalReferenceDeAprobado(paymentId, null);
                    if (externalRef != null) {
                        Integer appointmentId = Integer.parseInt(externalRef);
                        appointmentService.confirmarPago(appointmentId);
                        log.info("Turno {} confirmado tras pago MP {} (webhook genérico)", appointmentId, paymentId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook MP genérico: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/webhook")
    public ResponseEntity<Void> webhookPing() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/webhook/{therapistId}")
    public ResponseEntity<Void> webhookPingTerapeuta(@PathVariable Integer therapistId) {
        return ResponseEntity.ok().build();
    }
}
