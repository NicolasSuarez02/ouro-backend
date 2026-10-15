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

    List<TimeSlot> findByTherapistIdAndStatusInAndStartAtBetween(
            Integer therapistId, List<TimeSlot.SlotStatus> statuses, LocalDateTime from, LocalDateTime to);

    List<TimeSlot> findByTherapistIdAndStatusAndStartAtAfter(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime after);

    @Query("SELECT s FROM TimeSlot s WHERE s.therapist.id = :therapistId AND s.endAt > :from AND s.startAt < :to")
    List<TimeSlot> findOverlapping(@Param("therapistId") Integer therapistId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    void deleteByTherapistIdAndStatusAndStartAtAfter(
            Integer therapistId, TimeSlot.SlotStatus status, LocalDateTime after);

    Optional<TimeSlot> findByAppointmentId(Integer appointmentId);

    List<TimeSlot> findByTherapistId(Integer therapistId);

    void deleteByTherapistId(Integer therapistId);

    @Query("SELECT MAX(s.startAt) FROM TimeSlot s WHERE s.therapist.id = :therapistId AND s.status = 'FREE'")
    Optional<LocalDateTime> findMaxFreeSlotStartAt(@Param("therapistId") Integer therapistId);
}
