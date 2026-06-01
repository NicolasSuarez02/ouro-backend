package com.ouro.service;

import com.ouro.dto.AppointmentDTO;
import com.ouro.entity.Appointment;
import com.ouro.entity.Client;
import com.ouro.entity.TherapistSpecialty;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final EmailService emailService;

    private static final ZoneId ART = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              TherapistRepository therapistRepository,
                              UserRepository userRepository,
                              PaymentService paymentService,
                              ZoomService zoomService,
                              EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.therapistRepository = therapistRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.zoomService = zoomService;
        this.emailService = emailService;
    }

    /**
     * Días del mes que tienen al menos un time slot FREE para el terapeuta.
     * Retorna lista de fechas "yyyy-MM-dd".
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableDaysInMonth(Integer therapistId, int year, int month, String specialtyName) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        LocalDateTime from = startOfMonth.atStartOfDay();
        LocalDateTime until = endOfMonth.atTime(23, 59, 59);

        com.ouro.entity.Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));
        int leadHours = getLeadHours(therapist, specialtyName);
        LocalDateTime minStartAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(leadHours);

        return timeSlotRepository
                .findByTherapistIdAndStatusAndStartAtBetween(
                        therapistId, TimeSlot.SlotStatus.FREE, from, until)
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
    public List<AppointmentDTO.SlotResponse> getAvailableSlotsForDay(Integer therapistId, LocalDate date, String specialtyName) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime until = date.atTime(23, 59, 59);

        com.ouro.entity.Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado"));
        int leadHours = getLeadHours(therapist, specialtyName);
        LocalDateTime minStartAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(leadHours);

        return timeSlotRepository
                .findByTherapistIdAndStatusAndStartAtBetween(
                        therapistId, TimeSlot.SlotStatus.FREE, from, until)
                .stream()
                .filter(slot -> slot.getStartAt().isAfter(minStartAt))
                .map(AppointmentDTO.SlotResponse::new)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    /**
     * Regenera la URL de pago para un turno que está en PENDING_PAYMENT.
     * Solo puede pedirlo el usuario dueño del turno.
     */
    @Transactional(readOnly = true)
    public String getPaymentLink(Integer appointmentId, Integer userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tenés permiso para ver este turno");
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.PENDING_PAYMENT) {
            throw new RuntimeException("El turno no está pendiente de pago");
        }

        try {
            String therapistToken = appointment.getTherapist().getMpAccessToken();
            return paymentService.createPaymentPreference(appointment, therapistToken);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el link de pago: " + e.getMessage());
        }
    }

    private void notifyTherapistOfNewAppointment(Appointment appointment) {
        try {
            User client = appointment.getUser();
            Client clientProfile = client.getClient();

            String birthDate = null;
            String birthTime = null;
            if (clientProfile != null) {
                if (clientProfile.getDateOfBirth() != null) {
                    birthDate = clientProfile.getDateOfBirth().toLocalDateTime().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
                if (clientProfile.getTimeOfBirth() != null) {
                    birthTime = clientProfile.getTimeOfBirth().format(TIME_FMT);
                }
            }

            String appointmentDate = appointment.getStartAt().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ART).format(DATE_FMT);
            String appointmentTime = appointment.getStartAt().atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ART).format(TIME_FMT);

            emailService.sendNewAppointmentNotificationToTherapist(
                    appointment.getTherapist().getUser().getEmail(),
                    client.getFullName(),
                    client.getEmail(),
                    client.getPhone(),
                    birthDate,
                    birthTime,
                    appointment.getSpecialtyName(),
                    appointmentDate,
                    appointmentTime,
                    appointment.getZoomJoinUrl()
            );
        } catch (Exception e) {
            log.error("Error al notificar al terapeuta sobre turno {}: {}", appointment.getId(), e.getMessage());
        }
    }

    private int getLeadHours(com.ouro.entity.Therapist therapist, String specialtyName) {
        int defaultHours = therapist.getMinBookingLeadHours() != null ? therapist.getMinBookingLeadHours() : 1;
        if (specialtyName == null || specialtyName.isBlank()) return defaultHours;
        return therapist.getSpecialties().stream()
                .filter(s -> s.getName().equalsIgnoreCase(specialtyName))
                .findFirst()
                .map(TherapistSpecialty::getMinBookingLeadHours)
                .orElse(defaultHours);
    }

    /**
     * El usuario reserva un turno seleccionando un time slot.
     * Si el terapeuta tiene precio > 0, el turno queda en PENDING_PAYMENT
     * y se retorna una URL de pago de Mercado Pago.
     * El userId viene del JWT (extraído en el controller).
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse bookAppointment(AppointmentDTO.BookAppointmentRequest request, Integer userId) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.getTimeSlotId())
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + request.getTimeSlotId()));

        if (slot.getStatus() != TimeSlot.SlotStatus.FREE) {
            throw new RuntimeException("El turno ya no está disponible");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        String rawSpecialty = (request.getSpecialtyName() != null && !request.getSpecialtyName().isBlank())
                ? request.getSpecialtyName() : null;

        List<TherapistSpecialty> specList = slot.getTherapist().getSpecialties();
        Integer price;
        String specialtyName;
        if (rawSpecialty != null) {
            specialtyName = rawSpecialty;
            price = specList.stream()
                    .filter(s -> s.getName().equalsIgnoreCase(specialtyName))
                    .findFirst()
                    .map(TherapistSpecialty::getPriceAmountCents)
                    .orElse(0);
        } else if (specList.size() == 1) {
            specialtyName = specList.get(0).getName();
            price = specList.get(0).getPriceAmountCents();
        } else {
            specialtyName = null;
            price = 0;
        }

        if (price > 0) {
            // Sesión de pago: crear preferencia MP sin bloquear el slot.
            // El appointment se crea recién cuando el webhook confirma el pago.
            AppointmentDTO.AppointmentResponse response = new AppointmentDTO.AppointmentResponse();
            try {
                String paymentUrl = paymentService.createBookingPreference(
                        slot.getId(), userId, specialtyName, price,
                        slot.getTherapist().getPriceCurrency(),
                        slot.getTherapist().getUser().getFullName(),
                        slot.getTherapist().getId(),
                        user.getEmail(),
                        slot.getTherapist().getMpAccessToken());
                response.setPaymentUrl(paymentUrl);
            } catch (Exception e) {
                log.error("Error al crear preferencia MP: {}", e.getMessage());
                throw new RuntimeException("No se pudo iniciar el proceso de pago. Intentá nuevamente.");
            }
            return response;
        }

        // Sesión gratuita: reservar directo
        Appointment appointment = new Appointment();
        appointment.setTherapist(slot.getTherapist());
        appointment.setUser(user);
        appointment.setStartAt(slot.getStartAt());
        appointment.setEndAt(slot.getEndAt());
        appointment.setStatus(Appointment.AppointmentStatus.RESERVED);
        appointment.setPriceAmountCents(0);
        appointment.setCurrency(slot.getTherapist().getPriceCurrency());
        appointment.setNotes(request.getNotes());
        if (specialtyName != null) appointment.setSpecialtyName(specialtyName);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        slot.setStatus(TimeSlot.SlotStatus.RESERVED);
        slot.setAppointment(savedAppointment);
        timeSlotRepository.save(slot);

        return new AppointmentDTO.AppointmentResponse(savedAppointment);
    }

    /**
     * Crea un appointment RESERVED a partir del externalReference del webhook MP.
     * Formato: BOOK|{slotId}|{userId}|{priceAmountCents}|{currency}|{encodedSpecialty}
     */
    @Transactional
    public void createAppointmentFromBookingRef(String externalRef) {
        String[] parts = externalRef.split("\\|", 6);
        if (parts.length < 5) {
            log.error("externalRef inválido: {}", externalRef);
            return;
        }
        Integer slotId = Integer.parseInt(parts[1]);
        Integer userId = Integer.parseInt(parts[2]);
        Integer priceAmountCents = Integer.parseInt(parts[3]);
        String currency = parts[4];
        String specialtyName = null;
        if (parts.length > 5 && !parts[5].isBlank()) {
            specialtyName = URLDecoder.decode(parts[5], StandardCharsets.UTF_8);
        }

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new RuntimeException("Slot no encontrado: " + slotId));

        if (slot.getStatus() != TimeSlot.SlotStatus.FREE) {
            log.error("Slot {} ya no está libre al confirmar pago. Requiere revisión manual.", slotId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));

        Appointment appointment = new Appointment();
        appointment.setTherapist(slot.getTherapist());
        appointment.setUser(user);
        appointment.setStartAt(slot.getStartAt());
        appointment.setEndAt(slot.getEndAt());
        appointment.setStatus(Appointment.AppointmentStatus.RESERVED);
        appointment.setPriceAmountCents(priceAmountCents);
        appointment.setCurrency(currency);
        if (specialtyName != null) appointment.setSpecialtyName(specialtyName);

        Appointment saved = appointmentRepository.save(appointment);
        slot.setStatus(TimeSlot.SlotStatus.RESERVED);
        slot.setAppointment(saved);
        timeSlotRepository.save(slot);

        ZoomService.ZoomMeetingUrls zoomUrls = zoomService.createMeeting(saved);
        if (zoomUrls != null) {
            saved.setZoomJoinUrl(zoomUrls.joinUrl());
            saved.setZoomStartUrl(zoomUrls.startUrl());
            appointmentRepository.save(saved);
        }

        notifyTherapistOfNewAppointment(saved);
        log.info("Appointment {} creado desde pago confirmado (slot {})", saved.getId(), slotId);
    }

    /**
     * Confirma el pago de un turno: cambia status a RESERVED y crea el meeting de Zoom.
     * Llamado desde el webhook de Mercado Pago o desde el endpoint de simulación.
     */
    @Transactional
    public AppointmentDTO.AppointmentResponse confirmPayment(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        if (appointment.getStatus() != Appointment.AppointmentStatus.PENDING_PAYMENT) {
            return new AppointmentDTO.AppointmentResponse(appointment);
        }

        appointment.setStatus(Appointment.AppointmentStatus.RESERVED);

        ZoomService.ZoomMeetingUrls zoomUrls = zoomService.createMeeting(appointment);
        if (zoomUrls != null) {
            appointment.setZoomJoinUrl(zoomUrls.joinUrl());
            appointment.setZoomStartUrl(zoomUrls.startUrl());
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Notificar al terapeuta con los datos del cliente
        notifyTherapistOfNewAppointment(saved);

        return new AppointmentDTO.AppointmentResponse(saved);
    }

    /**
     * Retorna un turno por ID. Solo puede verlo el usuario dueño, el terapeuta o un admin.
     */
    @Transactional(readOnly = true)
    public AppointmentDTO.AppointmentResponse getTurnoPorId(Integer id, Integer requestingUserId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + id));

        boolean isOwner = appointment.getUser().getId().equals(requestingUserId);
        boolean isTherapist = appointment.getTherapist().getUser().getId().equals(requestingUserId);

        if (!isOwner && !isTherapist) {
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
    public AppointmentDTO.AppointmentResponse cancelAppointment(Integer appointmentId, Integer userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        boolean isOwner = appointment.getUser().getId().equals(userId);
        boolean isTherapist = appointment.getTherapist().getUser().getId().equals(userId);

        if (!isOwner && !isTherapist) {
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
    public AppointmentDTO.AppointmentResponse completeAppointment(Integer appointmentId, Integer userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado con id: " + appointmentId));

        boolean isTherapist = appointment.getTherapist().getUser().getId().equals(userId);
        if (!isTherapist) {
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
    public AppointmentDTO.AgendaResponse getAppointmentsByUser(Integer userId, Integer requestingUserId) {
        if (!userId.equals(requestingUserId)) {
            User requesting = userRepository.findById(requestingUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            if (requesting.getRole() != User.Role.ADMIN) {
                throw new RuntimeException("No tenés permiso para ver estos turnos");
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<AppointmentDTO.AppointmentResponse> all = appointmentRepository
                .findByUserIdOrderByStartAtDesc(userId).stream()
                .map(AppointmentDTO.AppointmentResponse::new)
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> upcoming = all.stream()
                .filter(a -> !a.getStatus().equals("CANCELLED") && !a.getStatus().equals("COMPLETED")
                        && a.getStartAt() != null && LocalDateTime.parse(a.getStartAt()).isAfter(now))
                .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> past = all.stream()
                .filter(a -> a.getStatus().equals("CANCELLED") || a.getStatus().equals("COMPLETED")
                        || (a.getStartAt() != null && !LocalDateTime.parse(a.getStartAt()).isAfter(now)))
                .collect(Collectors.toList());

        return new AppointmentDTO.AgendaResponse(upcoming, past);
    }

    /**
     * Agenda de un terapeuta separada en próximos y pasados. Solo puede verla el propio terapeuta o un admin.
     */
    @Transactional(readOnly = true)
    public AppointmentDTO.AgendaResponse getAppointmentsByTherapist(Integer therapistId, Integer requestingUserId) {
        therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + therapistId));

        User requesting = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean isTherapist = therapistRepository.findById(therapistId)
                .map(t -> t.getUser().getId().equals(requestingUserId))
                .orElse(false);

        if (!isTherapist && requesting.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("No tenés permiso para ver la agenda de este terapeuta");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<AppointmentDTO.AppointmentResponse> all = appointmentRepository
                .findByTherapistIdOrderByStartAtAsc(therapistId).stream()
                .map(AppointmentDTO.AppointmentResponse::new)
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> upcoming = all.stream()
                .filter(a -> !a.getStatus().equals("CANCELLED") && !a.getStatus().equals("COMPLETED")
                        && a.getStartAt() != null && LocalDateTime.parse(a.getStartAt()).isAfter(now))
                .collect(Collectors.toList());

        List<AppointmentDTO.AppointmentResponse> past = all.stream()
                .filter(a -> a.getStatus().equals("CANCELLED") || a.getStatus().equals("COMPLETED")
                        || (a.getStartAt() != null && !LocalDateTime.parse(a.getStartAt()).isAfter(now)))
                .sorted((a, b) -> b.getStartAt().compareTo(a.getStartAt()))
                .collect(Collectors.toList());

        return new AppointmentDTO.AgendaResponse(upcoming, past);
    }
}
