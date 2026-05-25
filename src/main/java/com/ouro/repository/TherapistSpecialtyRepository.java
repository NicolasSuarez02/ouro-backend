package com.ouro.repository;

import com.ouro.entity.TherapistSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TherapistSpecialtyRepository extends JpaRepository<TherapistSpecialty, Integer> {
    List<TherapistSpecialty> findByTherapistId(Integer therapistId);

    @Transactional
    void deleteByTherapistId(Integer therapistId);
}
