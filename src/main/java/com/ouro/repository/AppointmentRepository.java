package com.ouro.repository;

import com.ouro.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByUserId(Integer userId);

    List<Appointment> findByTherapistId(Integer therapistId);

    List<Appointment> findByUserIdOrderByStartAtDesc(Integer userId);

    List<Appointment> findByTherapistIdOrderByStartAtAsc(Integer therapistId);

    void deleteByTherapistId(Integer therapistId);

    void deleteByUserId(Integer userId);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'PENDING_PAYMENT' AND a.startAt < :cutoff")
    List<Appointment> findPendingPaymentBefore(@Param("cutoff") LocalDateTime cutoff);
}
