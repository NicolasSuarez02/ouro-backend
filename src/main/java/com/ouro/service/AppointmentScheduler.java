package com.ouro.service;

import com.ouro.entity.Appointment;
import com.ouro.entity.TimeSlot;
import com.ouro.repository.AppointmentRepository;
import com.ouro.repository.TimeSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class AppointmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentScheduler.class);
    private static final ZoneId ART = ZoneId.of("America/Argentina/Buenos_Aires");

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PaymentService paymentService;
    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentScheduler(AppointmentRepository appointmentRepository,
                                TimeSlotRepository timeSlotRepository,
                                PaymentService paymentService,
                                AppointmentService appointmentService) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.paymentService = paymentService;
        this.appointmentService = appointmentService;
    }

    /**
     * Cancela turnos en PENDING_PAYMENT cuya fecha ya pasó (el cliente no pagó a tiempo).
     * Corre cada hora.
     */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cancelExpiredAppointments() {
        LocalDateTime now = LocalDateTime.now(ART);
        List<Appointment> expired = appointmentRepository.findPendingPaymentBefore(now);

        if (expired.isEmpty()) return;

        log.info("Cancelando {} turno(s) PENDING_PAYMENT vencidos", expired.size());

        for (Appointment appt : expired) {
            appt.setStatus(Appointment.AppointmentStatus.CANCELLED);
            appointmentRepository.save(appt);

            timeSlotRepository.findByAppointmentId(appt.getId()).ifPresent(slot -> {
                slot.setStatus(TimeSlot.SlotStatus.FREE);
                slot.setAppointment(null);
                timeSlotRepository.save(slot);
            });

            log.info("Turno {} cancelado (pago no recibido antes de {})", appt.getId(), appt.getStartAt());
        }
    }

    /**
     * Busca turnos PENDING_PAYMENT con más de 5 minutos de antigüedad y consulta MP
     * para ver si el pago fue aprobado pero el webhook no llegó. Corre cada 5 minutos.
     */
    @Scheduled(fixedDelay = 300_000)
    public void reconcilePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now(ART).minusMinutes(5);
        List<Appointment> pending = appointmentRepository.findPaidPendingPaymentsOlderThan(cutoff);
        if (pending.isEmpty()) return;

        log.info("Reconciliación: {} turno(s) en PENDING_PAYMENT para verificar", pending.size());

        for (Appointment apt : pending) {
            try {
                String token = apt.getTherapist().getMpAccessToken();
                if (token == null || token.isBlank()) continue;
                if (paymentService.hasApprovedPaymentForAppointment(apt.getId(), token)) {
                    appointmentService.confirmPayment(apt.getId());
                    log.info("Turno {} confirmado vía reconciliación (webhook perdido)", apt.getId());
                }
            } catch (Exception e) {
                log.error("Error reconciliando turno {}: {}", apt.getId(), e.getMessage());
            }
        }
    }
}
