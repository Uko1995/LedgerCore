package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.model.Reservation;
import com.example.LedgerCore.model.ReservationStatus;
import com.example.LedgerCore.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository repository;

    @Transactional
    public ReserveResponse reserve(ReserveRequest req) {
        String reservationId = "RES-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);

        Reservation r = Reservation.builder()
                .reservationId(reservationId)
                .transactionId(req.getTransactionId())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .merchantId(req.getMerchantId())
                .status(ReservationStatus.RESERVED)
                .build();

        r = repository.save(r);

        log.info("Reserved: reservationId={}, transactionId={}, amount={}, currency={}", r.getReservationId(), r.getTransactionId(), r.getAmount(), r.getCurrency());

        return ReserveResponse.builder()
                .reservationId(r.getReservationId())
                .status(r.getStatus().name())
                .message("Reserved")
                .build();
    }

    @Transactional
    public void release(String reservationId) {
        int updated = repository.releaseReservation(reservationId, ReservationStatus.RELEASED, ReservationStatus.RELEASED, Instant.now());
        if (updated == 0) {
            if (repository.existsByReservationId(reservationId)) {
                log.warn("Release attempted on already released reservation: reservationId={}", reservationId);
                throw new IllegalStateException("Reservation already released");
            }
            throw new EntityNotFoundException("Reservation not found");
        }
        log.info("Released: reservationId={}", reservationId);
    }
}

