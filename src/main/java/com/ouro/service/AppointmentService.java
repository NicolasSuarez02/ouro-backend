package com.ouro.service;

import com.ouro.dto.AppointmentDTO;
import com.ouro.entity.Appointment;
import com.ouro.entity.TimeSlot;
import com.ouro.entity.User;
import com.ouro.repository.AppointmentRepository;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.TimeSlotRepository;
import com.ouro.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TherapistRepository therapistRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ZoomService zoomService;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              TherapistRepository therapistRepository,
                              UserRepository userRepository,
                              PaymentService paymentService,
                              ZoomService zoomService) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.therapistRepository = therapistRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.zoomService = zoomService;
    }

    /**
     * Días del mes que tienen al menos un time slot FREE para el terapeuta.
     * Retorna lista de fechas "yyyy-MM-dd".
     */
    @Transactional(readOnly = true)
    public List<String> getDiasDisponiblesEnMes(Integer therapistId, int year, int month) {
        LocalDate inicioMes = LocalDate.of(year, month, 1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

        LocalDateTime desde = inicioMes.atStartOfDay();
        LocalDateTime hasta = finMes.atTime(23, 59, 59);

        com.ouro.entity.Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));
        int leadHours = therapist.getMinBookingLeadHours() != null ? therapist.getMinBookingLeadHours() : 1;
        LocalDateTime minStartAt = LocalDateTime.now().plusHours(leadHours);

        return timeSlotRepository
                .findByTherapistIdAndStatusAndStartAtBetween(
                        therapistId, TimeSlot.SlotStatus.FREE, desde, hasta)
                .stream()
                .filter(slot -> slot.getStartAt().isAfter(minStartAt))
                .map(slot -> slot.getStartAt().toLocalDate().toString())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Slots FREE disponibles de un terapeuta para un día específico.
     */
    @Transactional(readOnly = true)
    public List<AppointmentDTO.SlotResponse> getSlotsDisponiblesPorDia(Integer therapistId, LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(23, 59, 59);

        com.ouro.entity.Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));
        int leadHours = therapist.getMinBookingLeadHours() != null ? therapist.getMinBookingLeadHours() : 1;
        LocalDateTime minStartAt = LocalDateTime.now().plusHours(leadHours);

        return timeSlotRepository
                .findByTherapistIdAndStatusAndStartAtBetween(
                        therapistId, TimeSlot.SlotStatus.FREE, desde, hasta)
                .stream()
                .filter(slot -> slot.getStartAt().isAfter(minStartAt))
                .map(AppointmentDTO.SlotResponse::new)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    /**
     * El usuario reserva un turno seleccionando un time slot.
     * Si el terapeuta tiene precio > 0, el turno queda en PENDING_PAYMENT
     * y se retorna una URL de pago de Mercado Pago.
     * El userId viene del JWT (extraído en el controller).
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse reservarTurno(AppointmentDTO.BookAppointmentRequest request, Integer userId) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.getTimeSlotId())
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + request.getTimeSlotId()));

        if (slot.getStatus() != TimeSlot.SlotStatus.FREE) {
            throw new RuntimeException("El turno ya no está disponible");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        Integer precio = slot.getTherapist().getPriceAmountCents();
        boolean tienePrecio = precio != null && precio > 0;

        Appointment appointment = new Appointment();
        appointment.setTherapist(slot.getTherapist());
        appointment.setUser(user);
        appointment.setStartAt(slot.getStartAt());
        appointment.setEndAt(slot.getEndAt());
        appointment.setStatus(tienePrecio
                ? Appointment.AppointmentStatus.PENDING_PAYMENT
                : Appointment.AppointmentStatus.RESERVED);
        appointment.setPriceAmountCents(precio != null ? precio : 0);
        appointment.setCurrency(slot.getTherapist().getPriceCurrency());
        appointment.setNotes(request.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Marcar el slot como reservado
        slot.setStatus(TimeSlot.SlotStatus.RESERVED);
        slot.setAppointment(savedAppointment);
        timeSlotRepository.save(slot);

        AppointmentDTO.AppointmentResponse response = new AppointmentDTO.AppointmentResponse(savedAppointment);

        // Crear preferencia de pago en Mercado Pago usando el token del terapeuta
        if (tienePrecio) {
            try {
                String tokenTerapeuta = slot.getTherapist().getMpAccessToken();
                String paymentUrl = paymentService.crearPreferenciaMP(savedAppointment, tokenTerapeuta);
                response.setPaymentUrl(paymentUrl);
            } catch (Exception e) {
                log.error("Error al crear preferencia MP para turno {}: {}", savedAppointment.getId(), e.getMessage());
                // No bloqueamos la reserva; el usuario puede reintentar el pago
            }
        }

        return response;
    }

    /**
     * Confirma el pago de un turno: cambia status a RESERVED y crea el meeting de Zoom.
     * Llamado desde el webhook de Mercado Pago o desde el endpoint de simulación.
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse confirmarPago(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        if (appointment.getStatus() != Appointment.AppointmentStatus.PENDING_PAYMENT) {
            return new AppointmentDTO.AppointmentResponse(appointment);
        }

        appointment.setStatus(Appointment.AppointmentStatus.RESERVED);

        // Crear meeting de Zoom
        String zoomUrl = zoomService.crearMeeting(appointment);
        if (zoomUrl != null) {
            appointment.setZoomJoinUrl(zoomUrl);
        }

        return new AppointmentDTO.AppointmentResponse(appointmentRepository.save(appointment));
    }

    /**
     * Retorna un turno por ID. Solo puede verlo el usuario dueño, el terapeuta o un admin.
     */
    @Transactional(readOnly = true)
    public AppointmentDTO.AppointmentResponse getTurnoPorId(Integer id, Integer requestingUserId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + id));

        boolean esElUsuario = appointment.getUser().getId().equals(requestingUserId);
        boolean esElTerapeuta = appointment.getTherapist().getUser().getId().equals(requestingUserId);

        if (!esElUsuario && !esElTerapeuta) {
            User requesting = userRepository.findById(requestingUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            if (requesting.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("No tenés permiso para ver este turno");
            }
        }

        return new AppointmentDTO.AppointmentResponse(appointment);
    }

    /**
     * Cancela un turno. Solo puede hacerlo el usuario que lo reservó o el terapeuta.
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse cancelarTurno(Integer appointmentId, Integer userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        boolean esElUsuario = appointment.getUser().getId().equals(userId);
        boolean esElTerapeuta = appointment.getTherapist().getUser().getId().equals(userId);

        if (!esElUsuario && !esElTerapeuta) {
            throw new RuntimeException("No tenés permiso para cancelar este turno");
        }

        if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED) {
            throw new RuntimeException("El turno ya está cancelado");
        }

        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        // Liberar el time slot
        timeSlotRepository.findByAppointmentId(appointmentId).ifPresent(slot -> {
            slot.setStatus(TimeSlot.SlotStatus.FREE);
            slot.setAppointment(null);
            timeSlotRepository.save(slot);
        });

        return new AppointmentDTO.AppointmentResponse(appointment);
    }

    /**
     * Marca un turno como completado. Solo puede hacerlo el terapeuta del turno.
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse completarTurno(Integer appointmentId, Integer userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        boolean esElTerapeuta = appointment.getTherapist().getUser().getId().equals(userId);
        if (!esElTerapeuta) {
            throw new RuntimeException("Solo el terapeuta puede marcar el turno como completado");
        }

        if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED) {
            throw new RuntimeException("No se puede completar un turno cancelado");
        }

        appointment.setStatus(Appointment.AppointmentStatus.COMPLETED);
        return new AppointmentDTO.AppointmentResponse(appointmentRepository.save(appointment));
    }

    /**
     * Historial de turnos de un usuario, separados en próximos y pasados.
     */
    @Transactional(readOnly = true)
    public AppointmentDTO.AgendaResponse getTurnosPorUsuario(Integer userId, Integer requestingUserId) {
        if (!userId.equals(requestingUserId)) {
            User requesting = userRepository.findById(requestingUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            if (requesting.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("No tenés permiso para ver estos turnos");
            }
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<AppointmentDTO.AppointmentResponse> todos = appointmentRepository
                .findByUserIdOrderByStartAtDesc(userId).stream()
                .map(AppointmentDTO.AppointmentResponse::new)
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> proximos = todos.stream()
                .filter(a -> !a.getStatus().equals("CANCELLED") && !a.getStatus().equals("COMPLETED")
                        && a.getStartAt() != null && LocalDateTime.parse(a.getStartAt()).isAfter(ahora))
                .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> pasados = todos.stream()
                .filter(a -> a.getStatus().equals("CANCELLED") || a.getStatus().equals("COMPLETED")
                        || (a.getStartAt() != null && !LocalDateTime.parse(a.getStartAt()).isAfter(ahora)))
                .collect(Collectors.toList());

        return new AppointmentDTO.AgendaResponse(proximos, pasados);
    }

    /**
     * Agenda de un terapeuta separada en próximos y pasados. Solo puede verla el propio terapeuta o un admin.
     */
    @Transactional(readOnly = true)
    public AppointmentDTO.AgendaResponse getTurnosPorTerapeuta(Integer therapistId, Integer requestingUserId) {
        therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + therapistId));

        User requesting = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean esTerapeuta = therapistRepository.findById(therapistId)
                .map(t -> t.getUser().getId().equals(requestingUserId))
                .orElse(false);

        if (!esTerapeuta && requesting.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("No tenés permiso para ver la agenda de este terapeuta");
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<AppointmentDTO.AppointmentResponse> todos = appointmentRepository
                .findByTherapistIdOrderByStartAtAsc(therapistId).stream()
                .map(AppointmentDTO.AppointmentResponse::new)
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> proximos = todos.stream()
                .filter(a -> !a.getStatus().equals("CANCELLED") && !a.getStatus().equals("COMPLETED")
                        && a.getStartAt() != null && LocalDateTime.parse(a.getStartAt()).isAfter(ahora))
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> pasados = todos.stream()
                .filter(a -> a.getStatus().equals("CANCELLED") || a.getStatus().equals("COMPLETED")
                        || (a.getStartAt() != null && !LocalDateTime.parse(a.getStartAt()).isAfter(ahora)))
                .sorted((a, b) -> b.getStartAt().compareTo(a.getStartAt()))
                .collect(Collectors.toList());

        return new AppointmentDTO.AgendaResponse(proximos, pasados);
    }
}
