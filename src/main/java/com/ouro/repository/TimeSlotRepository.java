package com.ouro.repository;

import com.ouro.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {

    List<TimeSlot> findByTherapistIdAndStatusAndStartAtBetween(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime from, LocalDateTime to);

    void deleteByTherapistIdAndStatusAndStartAtAfter(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime after);

    Optional<TimeSlot> findByAppointmentId(Integer appointmentId);

    List<TimeSlot> findByTherapistId(Integer therapistId);
}
