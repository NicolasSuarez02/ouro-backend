package com.ouro.repository;

import com.ouro.entity.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TimeSlot s WHERE s.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Integer id);

    List<TimeSlot> findByTherapistIdAndStatusAndStartAtBetween(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime from, LocalDateTime to);

    void deleteByTherapistIdAndStatusAndStartAtAfter(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime after);

    Optional<TimeSlot> findByAppointmentId(Integer appointmentId);

    List<TimeSlot> findByTherapistId(Integer therapistId);

    void deleteByTherapistId(Integer therapistId);
}
