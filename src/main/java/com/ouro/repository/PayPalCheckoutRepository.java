package com.ouro.repository;

import com.ouro.entity.PayPalCheckout;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayPalCheckoutRepository extends JpaRepository<PayPalCheckout, Integer> {

    // Bloquea la fila: si el retorno del cliente y el webhook llegan a la vez, el segundo espera
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PayPalCheckout c WHERE c.paypalOrderId = :orderId")
    Optional<PayPalCheckout> findByPaypalOrderIdForUpdate(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PayPalCheckout c WHERE c.paypalCaptureId = :captureId")
    Optional<PayPalCheckout> findByPaypalCaptureIdForUpdate(@Param("captureId") String captureId);
}
