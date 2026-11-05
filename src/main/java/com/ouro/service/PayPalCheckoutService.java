package com.ouro.service;

import com.ouro.config.CorsConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.ouro.dto.AppointmentDTO;
import com.ouro.entity.*;
import com.ouro.repository.AppointmentRepository;
import com.ouro.repository.PayPalCheckoutRepository;
import com.ouro.repository.TimeSlotRepository;
import com.ouro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reservas pagadas con PayPal. Regla central: el backend verifica que el horario siga libre
 * ANTES de cobrar. Si el horario ya no está, no se cobra nada.
 */
@Service
public class PayPalCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(PayPalCheckoutService.class);
    private static final ZoneId ART = ZoneId.of("America/Argentina/Buenos_Aires");

    public enum Outcome { CAPTURED, PENDING, SLOT_TAKEN, DECLINED, RETRY }

    /** Resultado de intentar cobrar una orden. appointment solo viene en CAPTURED y PENDING. */
    public record CaptureOutcome(Outcome outcome, AppointmentDTO.AppointmentResponse appointment) {}

    private final PayPalClient payPalClient;
    private final PayPalCheckoutRepository checkoutRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PayPalCheckoutService(PayPalClient payPalClient,
                                 PayPalCheckoutRepository checkoutRepository,
                                 TimeSlotRepository timeSlotRepository,
                                 UserRepository userRepository,
                                 AppointmentRepository appointmentRepository,
                                 AppointmentService appointmentService) {
        this.payPalClient = payPalClient;
        this.checkoutRepository = checkoutRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
    }

    /**
     * Crea la orden en PayPal para reservar un horario. Devuelve [orderId, approveUrl].
     * No bloquea el horario: se verifica de nuevo al cobrar.
     */
    public String[] createOrder(Integer userId, Integer timeSlotId, String specialtyName, String notes, String origin) {
        if (!payPalClient.isConfigured()) {
            throw new RuntimeException("El pago con PayPal no está disponible en este momento");
        }

        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new RuntimeException("El horario ya no está disponible"));
        if (slot.getStatus() != TimeSlot.SlotStatus.FREE) {
            throw new RuntimeException("El horario ya no está disponible");
        }

        Therapist therapist = slot.getTherapist();
        if (therapist.getApprovalStatus() != Therapist.ApprovalStatus.APPROVED) {
            throw new RuntimeException("Este terapeuta no está recibiendo reservas");
        }
        if (!therapist.isPaypalConnected()) {
            throw new RuntimeException("Este terapeuta no acepta pagos con PayPal");
        }

        TherapistSpecialty specialty = appointmentService.resolveSpecialty(therapist, specialtyName);
        if (specialty == null || specialty.getPriceUsdCents() == null || specialty.getPriceUsdCents() < 100) {
            throw new RuntimeException("Esta sesión no se puede pagar con PayPal");
        }

        int leadHours = appointmentService.leadHoursFor(therapist, specialty.getName());
        if (!slot.getStartAt().isAfter(LocalDateTime.now(ART).plusHours(leadHours))) {
            throw new RuntimeException("El horario ya no está disponible");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PayPalCheckout checkout = new PayPalCheckout();
        checkout.setTimeSlotId(slot.getId());
        checkout.setUserId(userId);
        checkout.setTherapistId(therapist.getId());
        checkout.setSpecialtyName(specialty.getName());
        checkout.setAmountUsdCents(specialty.getPriceUsdCents());
        checkout.setPayeeEmail(therapist.getPaypalEmail());
        checkout.setNotes(notes);
        checkout = checkoutRepository.save(checkout);

        // Vuelve al mismo sitio desde donde se pagó (ej. una preview de Vercel), si es un origen permitido
        String base = CorsConfig.isAllowedOrigin(origin) ? origin : frontendBase();
        String description = "Sesión de " + specialty.getName() + " con " + therapist.getUser().getFullName();
        try {
            String[] order = payPalClient.createOrder(checkout.getId(), checkout.getPayeeEmail(),
                    checkout.getAmountUsdCents(), description,
                    base + "/pago/paypal/retorno", base + "/pago/paypal/cancelado");
            checkout.setPaypalOrderId(order[0]);
            checkoutRepository.save(checkout);
            log.info("Orden PayPal {} creada (checkout {}, slot {}, user {})",
                    order[0], checkout.getId(), slot.getId(), userId);
            return order;
        } catch (RuntimeException e) {
            checkout.setStatus(PayPalCheckout.Status.FAILED);
            checkoutRepository.save(checkout);
            log.error("Error creando orden PayPal (checkout {}): {}", checkout.getId(), e.getMessage());
            throw new RuntimeException("No se pudo iniciar el pago con PayPal. Intentá nuevamente.");
        }
    }

    /**
     * Cobra una orden aprobada y crea el turno. La llaman la página de retorno (con el userId
     * del JWT) y el webhook (requestingUserId = null). Lo que llegue primero gana; el otro
     * recibe el mismo turno.
     */
    @Transactional
    public CaptureOutcome capture(String orderId, Integer requestingUserId) {
        PayPalCheckout checkout = checkoutRepository.findByPaypalOrderIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Orden de pago no encontrada"));

        if (requestingUserId != null && !checkout.getUserId().equals(requestingUserId)) {
            throw new RuntimeException("No tenés permiso para confirmar este pago");
        }

        switch (checkout.getStatus()) {
            case CAPTURED:
                return new CaptureOutcome(Outcome.CAPTURED, appointmentOf(checkout));
            case PENDING:
                return new CaptureOutcome(Outcome.PENDING, appointmentOf(checkout));
            case SLOT_TAKEN:
                return new CaptureOutcome(Outcome.SLOT_TAKEN, null);
            case FAILED:
            case DENIED:
                return new CaptureOutcome(Outcome.DECLINED, null);
            case CREATED:
            default:
                break;
        }

        // El horario queda bloqueado mientras se llama a PayPal (1 a 3 segundos)
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(checkout.getTimeSlotId()).orElse(null);
        if (slot == null || slot.getStatus() != TimeSlot.SlotStatus.FREE) {
            checkout.setStatus(PayPalCheckout.Status.SLOT_TAKEN);
            checkoutRepository.save(checkout);
            log.warn("Orden PayPal {}: el horario {} ya no está libre, no se cobra", orderId, checkout.getTimeSlotId());
            return new CaptureOutcome(Outcome.SLOT_TAKEN, null);
        }

        PayPalClient.CaptureResult result = payPalClient.captureOrder(orderId);
        switch (result.status()) {
            case "COMPLETED":
            case "PENDING":
                break;
            case "DECLINED":
                checkout.setStatus(PayPalCheckout.Status.FAILED);
                checkoutRepository.save(checkout);
                log.warn("Orden PayPal {} rechazada al cobrar: {}", orderId, result.detail());
                return new CaptureOutcome(Outcome.DECLINED, null);
            default:
                // Error de red o de PayPal: la orden queda CREATED para reintentar
                // (el reintento usa el mismo PayPal-Request-Id, así que no cobra dos veces)
                log.error("Orden PayPal {}: no se pudo confirmar el cobro ({})", orderId, result.detail());
                return new CaptureOutcome(Outcome.RETRY, null);
        }

        Integer chargedCents = PayPalClient.parseUsdCents(result.amountValue());
        if (!"USD".equals(result.currency()) || chargedCents == null
                || !chargedCents.equals(checkout.getAmountUsdCents())) {
            // No debería pasar (el monto lo fija el backend), pero se registra para revisar a mano
            log.error("Orden PayPal {}: monto cobrado {} {} distinto al esperado {} USD cents",
                    orderId, result.amountValue(), result.currency(), checkout.getAmountUsdCents());
        }

        User user = userRepository.findById(checkout.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Appointment appointment = appointmentService.createPaidAppointment(slot, user,
                checkout.getSpecialtyName(), checkout.getAmountUsdCents(), "USD", "PAYPAL",
                orderId, checkout.getNotes());
        appointment.setPaypalCaptureId(result.captureId());
        appointment = appointmentRepository.save(appointment);

        boolean pending = "PENDING".equals(result.status());
        checkout.setStatus(pending ? PayPalCheckout.Status.PENDING : PayPalCheckout.Status.CAPTURED);
        checkout.setPaypalCaptureId(result.captureId());
        checkout.setAppointmentId(appointment.getId());
        checkoutRepository.save(checkout);

        log.info("Orden PayPal {} cobrada ({}), turno {} creado", orderId, result.status(), appointment.getId());
        return new CaptureOutcome(pending ? Outcome.PENDING : Outcome.CAPTURED,
                new AppointmentDTO.AppointmentResponse(appointment));
    }

    /**
     * Procesa un evento de webhook ya verificado. Transaccional porque llama a capture() y a los
     * demás métodos desde adentro de la misma clase (sin pasar por el proxy de Spring).
     */
    @Transactional
    public void handleWebhookEvent(JsonNode event) {
        String type = event.path("event_type").asText();
        JsonNode resource = event.path("resource");
        log.info("Webhook PayPal recibido: {} (evento {})", type, event.path("id").asText());

        switch (type) {
            case "CHECKOUT.ORDER.APPROVED" -> {
                // Respaldo: el cliente aprobó pero no volvió a la página de retorno
                String orderId = resource.path("id").asText(null);
                if (orderId != null) {
                    CaptureOutcome outcome = capture(orderId, null);
                    log.info("Webhook PayPal: orden {} → {}", orderId, outcome.outcome());
                }
            }
            case "PAYMENT.CAPTURE.COMPLETED" -> markPendingCaptureCompleted(resource.path("id").asText(null));
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> cancelDeniedCapture(resource.path("id").asText(null));
            case "PAYMENT.CAPTURE.REFUNDED", "PAYMENT.CAPTURE.REVERSED" -> {
                String orderId = resource.path("supplementary_data").path("related_ids").path("order_id").asText("");
                // El turno no cambia solo: la administración decide qué hacer
                log.warn("ATENCIÓN: PayPal {} para captura {} (orden {}). Revisar el turno a mano.",
                        type, resource.path("id").asText(), orderId);
            }
            default -> log.debug("Webhook PayPal ignorado: {}", type);
        }
    }

    @Transactional
    public void markPendingCaptureCompleted(String captureId) {
        if (captureId == null) return;
        checkoutRepository.findByPaypalCaptureIdForUpdate(captureId).ifPresent(checkout -> {
            if (checkout.getStatus() == PayPalCheckout.Status.PENDING) {
                checkout.setStatus(PayPalCheckout.Status.CAPTURED);
                checkoutRepository.save(checkout);
                log.info("Captura PayPal {} completada tras revisión (turno {})", captureId, checkout.getAppointmentId());
            }
        });
    }

    @Transactional
    public void cancelDeniedCapture(String captureId) {
        if (captureId == null) return;
        checkoutRepository.findByPaypalCaptureIdForUpdate(captureId).ifPresent(checkout -> {
            if (checkout.getStatus() != PayPalCheckout.Status.PENDING) return;
            checkout.setStatus(PayPalCheckout.Status.DENIED);
            checkoutRepository.save(checkout);
            if (checkout.getAppointmentId() != null) {
                appointmentService.cancelUnpaidAppointment(checkout.getAppointmentId());
            }
            log.warn("Captura PayPal {} rechazada tras revisión: turno {} cancelado. Avisar al cliente (user {}).",
                    captureId, checkout.getAppointmentId(), checkout.getUserId());
        });
    }

    private AppointmentDTO.AppointmentResponse appointmentOf(PayPalCheckout checkout) {
        if (checkout.getAppointmentId() == null) return null;
        return appointmentRepository.findById(checkout.getAppointmentId())
                .map(AppointmentDTO.AppointmentResponse::new)
                .orElse(null);
    }

    private String frontendBase() {
        String base = frontendUrl.trim();
        if (!base.startsWith("http")) base = "https://" + base;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }
}
