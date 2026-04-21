package com.ouro.repository;

import com.ouro.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    List<Rating> findByTherapistIdOrderByCreatedAtDesc(Integer therapistId);

    Optional<Rating> findByUserIdAndTherapistId(Integer userId, Integer therapistId);

    boolean existsByUserIdAndTherapistId(Integer userId, Integer therapistId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.therapist.id = :therapistId")
    Double findAverageScoreByTherapistId(@Param("therapistId") Integer therapistId);

    long countByTherapistId(Integer therapistId);

    @Modifying
    @Query("DELETE FROM Rating r WHERE r.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM Rating r WHERE r.therapist.id = :therapistId")
    void deleteAllByTherapistId(@Param("therapistId") Integer therapistId);
}
