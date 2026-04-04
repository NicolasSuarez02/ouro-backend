package com.ouro.service;

import com.ouro.dto.AvailabilityDTO;
import com.ouro.entity.Availability;
import com.ouro.entity.Therapist;
import com.ouro.entity.TimeSlot;
import com.ouro.entity.User;
import com.ouro.repository.AvailabilityRepository;
import com.ouro.repository.TherapistRepository;
import com.ouro.repository.TimeSlotRepository;
import com.ouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TherapistRepository therapistRepository;
    private final UserRepository userRepository;

    @Autowired
    public AvailabilityService(AvailabilityRepository availabilityRepository,
                               TimeSlotRepository timeSlotRepository,
                               TherapistRepository therapistRepository,
                               UserRepository userRepository) {
        this.availabilityRepository = availabilityRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.therapistRepository = therapistRepository;
        this.userRepository = userRepository;
    }

    /**
     * Reemplaza toda la disponibilidad del terapeuta y regenera sus time slots futuros.
     * Solo puede hacerlo el propio terapeuta (verificado por userId).
     */
    @Transactional
    public List<AvailabilityDTO.AvailabilityResponse> saveAvailability(
            Integer therapistId,
            AvailabilityDTO.SaveAvailabilityRequest request) {

        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new RuntimeException("Terapeuta no encontrado con id: " + therapistId));

        // Verificar que el userId corresponde al terapeuta
        if (!therapist.getUser().getId().equals(request.getUserId())) {
            throw new RuntimeException("No tenés permiso para modificar la disponibilidad de este terapeuta");
        }

        // Borrar disponibilidad existente
        availabilityRepository.deleteByTherapistId(therapistId);

        // Borrar time slots futuros FREE (los RESERVED no se tocan)
        timeSlotRepository.deleteByTherapistIdAndStatusAndStartAtAfter(
                therapistId, TimeSlot.SlotStatus.FREE, LocalDateTime.now());

        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            return new ArrayList<>();
        }

        // Guardar nueva disponibilidad
        List<Availability> saved = new ArrayList<>();
        for (AvailabilityDTO.SlotRequest slotReq : request.getSlots()) {
            Availability avail = new Availability();
            avail.setTherapist(therapist);
            avail.setDayOfWeek(slotReq.getDayOfWeek());
            avail.setStartTime(LocalTime.parse(slotReq.getStartTime()));
            avail.setEndTime(LocalTime.parse(slotReq.getEndTime()));
            avail.setSlotDurationMinutes(
                    slotReq.getSlotDurationMinutes() != null ? slotReq.getSlotDurationMinutes() : 30);
            saved.add(availabilityRepository.save(avail));
        }

        // Generar time slots para los próximos 60 días
        generarTimeSlots(therapist, saved);

        return saved.stream()
                .map(AvailabilityDTO.AvailabilityResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDTO.AvailabilityResponse> getAvailabilityByTherapist(Integer therapistId) {
        return availabilityRepository.findByTherapistId(therapistId).stream()
                .map(AvailabilityDTO.AvailabilityResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Genera time slots para los próximos 60 días en base a la disponibilidad configurada.
     */
    private void generarTimeSlots(Therapist therapist, List<Availability> availabilities) {
        LocalDate hoy = LocalDate.now();
        LocalDate fin = hoy.plusDays(60);
        List<TimeSlot> slots = new ArrayList<>();

        for (LocalDate fecha = hoy; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            int dbDayOfWeek = convertirDiaADb(fecha.getDayOfWeek());

            for (Availability avail : availabilities) {
                if (!avail.getDayOfWeek().equals(dbDayOfWeek)) continue;
                if (avail.getDateFrom() != null && fecha.isBefore(avail.getDateFrom())) continue;
                if (avail.getDateTo() != null && fecha.isAfter(avail.getDateTo())) continue;

                LocalTime cursor = avail.getStartTime();
                int duracion = avail.getSlotDurationMinutes();

                while (!cursor.plusMinutes(duracion).isAfter(avail.getEndTime())) {
                    TimeSlot slot = new TimeSlot();
                    slot.setTherapist(therapist);
                    slot.setStartAt(LocalDateTime.of(fecha, cursor));
                    slot.setEndAt(LocalDateTime.of(fecha, cursor.plusMinutes(duracion)));
                    slot.setStatus(TimeSlot.SlotStatus.FREE);
                    slots.add(slot);
                    cursor = cursor.plusMinutes(duracion);
                }
            }
        }

        timeSlotRepository.saveAll(slots);
    }

    /**
     * Convierte DayOfWeek de Java a la convención DB (0=Domingo, 1=Lunes, ..., 6=Sábado).
     */
    private int convertirDiaADb(DayOfWeek dayOfWeek) {
        if (dayOfWeek == DayOfWeek.SUNDAY) return 0;
        return dayOfWeek.getValue(); // MONDAY=1 ... SATURDAY=6
    }
}
