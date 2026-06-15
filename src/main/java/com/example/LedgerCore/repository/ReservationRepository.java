package com.example.LedgerCore.repository;

import com.example.LedgerCore.model.Reservation;
import com.example.LedgerCore.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByReservationId(String reservationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.status = :newStatus, r.releasedAt = :now
            WHERE r.reservationId = :reservationId AND r.status <> :releasedStatus
            """)
    int releaseReservation(@Param("reservationId") String reservationId,
                           @Param("newStatus") ReservationStatus newStatus,
                           @Param("releasedStatus") ReservationStatus releasedStatus,
                           @Param("now") Instant now);

    boolean existsByReservationId(String reservationId);

    Optional<Reservation> findByTransactionId(String transactionId);
}

