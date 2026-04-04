package com.ouro.repository;

import com.ouro.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Integer> {
    
    Optional<Therapist> findByUserId(Integer userId);
    
    List<Therapist> findBySpecialty(String specialty);
    
    boolean existsByUserId(Integer userId);

    List<Therapist> findByApprovalStatus(Therapist.ApprovalStatus status);
}
