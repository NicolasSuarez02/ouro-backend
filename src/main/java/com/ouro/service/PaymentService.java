package com.ouro.service;

import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.ouro.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    private String resolveToken(String therapistToken) {
        if (therapistToken == null || therapistToken.isBlank()) {
            throw new IllegalStateException("El terapeuta no tiene configurado su token de Mercado Pago.");
        }
        return therapistToken;
    }

    /**
     * Crea una preferencia de pago en Mercado Pago usando el token del terapeuta.
     * La URL del webhook incluye el therapistId para poder identificar la cuenta en el callback.
     */
    public String createPaymentPreference(Appointment appointment, String therapistToken) throws MPException, MPApiException {
        String token = resolveToken(therapistToken);
        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .accessToken(token)
                .build();

        BigDecimal unitPrice = BigDecimal.valueOf(appointment.getPriceAmountCents())
                .divide(BigDecimal.valueOf(100));

        String therapistName = appointment.getTherapist().getUser().getFullName();
        Integer therapistId = appointment.getTherapist().getId();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Sesión de terapia - " + therapistName)
                .quantity(1)
                .unitPrice(unitPrice)
                .currencyId(appointment.getCurrency() != null ? appointment.getCurrency() : "ARS")
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/pago/exitoso?appointmentId=" + appointment.getId())
                .pending(frontendUrl + "/pago/pendiente?appointmentId=" + appointment.getId())
                .failure(frontendUrl + "/pago/fallido?appointmentId=" + appointment.getId())
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(PreferencePayerRequest.builder()
                        .email(appointment.getUser().getEmail())
                        .build())
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(appointment.getId().toString())
                // URL de webhook específica por terapeuta para saber qué token usar al validar
                .notificationUrl(backendUrl + "/api/payments/webhook/" + therapistId)
                .build();

        Preference preference = new PreferenceClient().create(request, requestOptions);

        return sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
    }

    /**
     * Extrae el ID del pago del body del webhook de Mercado Pago.
     */
    public Long extractPaymentIdFromWebhook(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object id = dataMap.get("id");
            if (id != null) {
                try {
                    return Long.parseLong(id.toString());
                } catch (NumberFormatException e) {
                    log.warn("ID de pago inválido en webhook: {}", id);
                }
            }
        }
        return null;
    }

    /**
     * Obtiene el external_reference de un pago aprobado usando el token del terapeuta.
     * Retorna null si el pago no está aprobado o si hay un error.
     */
    public String getExternalReferenceIfApproved(Long paymentId, String therapistToken) {
        String token = resolveToken(therapistToken);
        try {
            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(token)
                    .build();
            Payment payment = new PaymentClient().get(paymentId, requestOptions);
            log.info("Webhook pago {} status: {}", paymentId, payment.getStatus());
            if ("approved".equals(payment.getStatus())) {
                return payment.getExternalReference();
            }
        } catch (MPApiException e) {
            MPResponse resp = e.getApiResponse();
            log.error("Error MP al consultar pago {}. HTTP {}: {}", paymentId,
                    resp != null ? resp.getStatusCode() : "?",
                    resp != null ? resp.getContent() : e.getMessage());
        } catch (Exception e) {
            log.error("Error al consultar pago {} en MP: {}", paymentId, e.getMessage());
        }
        return null;
    }

    /**
     * Busca en MP si existe un pago aprobado con external_reference = appointmentId.
     * Usado por el scheduler de reconciliación para confirmar turnos cuyo webhook no llegó.
     */
    public boolean hasApprovedPaymentForAppointment(Integer appointmentId, String therapistToken) {
        try {
            String token = resolveToken(therapistToken);
            MPRequestOptions requestOptions = MPRequestOptions.builder().accessToken(token).build();
            Map<String, Object> filters = new HashMap<>();
            filters.put("external_reference", appointmentId.toString());
            MPSearchRequest searchRequest = MPSearchRequest.builder()
                    .limit(5)
                    .filters(filters)
                    .build();
            var results = new PaymentClient().search(searchRequest, requestOptions);
            if (results != null && results.getResults() != null) {
                return results.getResults().stream()
                        .anyMatch(p -> "approved".equals(p.getStatus()));
            }
        } catch (MPApiException e) {
            MPResponse resp = e.getApiResponse();
            log.error("Error MP buscando pago para turno {}. HTTP {}: {}", appointmentId,
                    resp != null ? resp.getStatusCode() : "?",
                    resp != null ? resp.getContent() : e.getMessage());
        } catch (Exception e) {
            log.error("Error buscando pago para turno {} en MP: {}", appointmentId, e.getMessage());
        }
        return false;
    }
}
