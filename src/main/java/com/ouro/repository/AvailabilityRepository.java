package com.ouro.repository;

import com.ouro.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Integer> {

    List<Availability> findByTherapistId(Integer therapistId);

    List<Availability> findByTherapistIdAndDayOfWeek(Integer therapistId, Integer dayOfWeek);

    void deleteByTherapistId(Integer therapistId);
}
