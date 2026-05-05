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
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AppointmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Autowired
    public AppointmentScheduler(AppointmentRepository appointmentRepository,
                                TimeSlotRepository timeSlotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Cancela turnos en PENDING_PAYMENT cuya fecha ya pasó (el cliente no pagó a tiempo).
     * Corre cada hora.
     */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cancelarTurnosVencidos() {
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        List<Appointment> vencidos = appointmentRepository.findPendingPaymentBefore(ahora);

        if (vencidos.isEmpty()) return;

        log.info("Cancelando {} turno(s) PENDING_PAYMENT vencidos", vencidos.size());

        for (Appointment appt : vencidos) {
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
}
