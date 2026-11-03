package com.ouro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ouro.dto.AppointmentDTO;
import com.ouro.service.PayPalCheckoutService;
import com.ouro.service.PayPalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Pagos de sesiones con PayPal. Mercado Pago sigue por su propio camino (PaymentController).
 */
@RestController
public class PayPalController {

    private static final Logger log = LoggerFactory.getLogger(PayPalController.class);

    private final PayPalCheckoutService checkoutService;
    private final PayPalClient payPalClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public PayPalController(PayPalCheckoutService checkoutService, PayPalClient payPalClient) {
        this.checkoutService = checkoutService;
        this.payPalClient = payPalClient;
    }

    /**
     * POST /api/appointments/paypal/order — el cliente elige pagar con PayPal.
     * Body: timeSlotId, specialtyName, notes. Devuelve orderId y approveUrl (a donde redirigir).
     */
    @PostMapping("/api/appointments/paypal/order")
    public ResponseEntity<Object> createOrder(@RequestBody AppointmentDTO.BookAppointmentRequest request) {
        try {
            if (request.getTimeSlotId() == null) {
                return error("Elegí un horario", HttpStatus.BAD_REQUEST);
            }
            String[] order = checkoutService.createOrder(currentUserId(), request.getTimeSlotId(),
                    request.getSpecialtyName(), request.getNotes());
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order[0]);
            result.put("approveUrl", order[1]);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST /api/appointments/paypal/order/{orderId}/capture — lo llama la página de retorno.
     * Verifica el horario, cobra y crea el turno. Si ya estaba cobrada, devuelve el mismo turno.
     */
    @PostMapping("/api/appointments/paypal/order/{orderId}/capture")
    public ResponseEntity<Object> capture(@PathVariable String orderId) {
        try {
            PayPalCheckoutService.CaptureOutcome outcome = checkoutService.capture(orderId, currentUserId());
            Map<String, Object> result = new HashMap<>();
            result.put("status", outcome.outcome().name());
            switch (outcome.outcome()) {
                case CAPTURED, PENDING -> {
                    result.put("appointment", outcome.appointment());
                    return ResponseEntity.ok(result);
                }
                case SLOT_TAKEN -> {
                    result.put("message", "Ese horario ya fue reservado por otra persona. No se te cobró nada.");
                    return new ResponseEntity<>(result, HttpStatus.CONFLICT);
                }
                case DECLINED -> {
                    result.put("message", "PayPal rechazó el pago. Probá con otro medio de pago.");
                    return new ResponseEntity<>(result, HttpStatus.PAYMENT_REQUIRED);
                }
                default -> {
                    result.put("message", "No pudimos confirmar el pago todavía. Reintentá en unos segundos.");
                    return new ResponseEntity<>(result, HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * POST /api/paypal/webhook — público, pero solo se procesa si PayPal confirma la firma.
     * Respaldo si el cliente cierra la pestaña antes de volver; registra reembolsos y contracargos.
     */
    @PostMapping("/api/paypal/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "paypal-auth-algo", required = false) String authAlgo,
            @RequestHeader(value = "paypal-cert-url", required = false) String certUrl,
            @RequestHeader(value = "paypal-transmission-id", required = false) String transmissionId,
            @RequestHeader(value = "paypal-transmission-sig", required = false) String transmissionSig,
            @RequestHeader(value = "paypal-transmission-time", required = false) String transmissionTime) {

        if (!payPalClient.verifyWebhookSignature(authAlgo, certUrl, transmissionId,
                transmissionSig, transmissionTime, rawBody)) {
            log.warn("Webhook PayPal con firma inválida (transmission {}), descartado", transmissionId);
            return ResponseEntity.badRequest().build();
        }
        try {
            JsonNode event = mapper.readTree(rawBody);
            checkoutService.handleWebhookEvent(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // 500 → PayPal reintenta el evento más tarde
            log.error("Error procesando webhook PayPal (transmission {}): {}", transmissionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Object> error(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
