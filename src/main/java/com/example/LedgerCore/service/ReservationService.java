package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.model.Account;
import com.example.LedgerCore.model.AccountStatus;
import com.example.LedgerCore.model.Reservation;
import com.example.LedgerCore.model.ReservationStatus;
import com.example.LedgerCore.repository.AccountRepository;
import com.example.LedgerCore.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository repository;
    private final AccountRepository accountRepository;

    @Transactional
    public ReserveResponse reserve(ReserveRequest req) {
        String transactionId = req.getTransactionId();
        String failureReason = null;

        Optional<Reservation> existing = repository.findByTransactionId(transactionId);
        if (existing.isPresent()) {
            Reservation r = existing.get();
            log.info("Idempotent request – returning existing reservation: reservationId={}, transactionId={}", r.getReservationId(), transactionId);
            return ReserveResponse.builder()
                    .reservationId(r.getReservationId())
                    .status(r.getStatus().name())
                    .message(r.getStatus() == ReservationStatus.FAILED ? "Failed" : "Reserved")
                    .reason(r.getStatus() == ReservationStatus.FAILED ? "Idempotent request – previous reservation already failed" : null)
                    .build();
        }

        Account account = accountRepository.findByMerchantIdWithLock(req.getMerchantId()).orElse(null);
        if (account == null) {
            failureReason = "Account not found for merchant: " + req.getMerchantId();
        } else if (account.getStatus() != AccountStatus.ACTIVE) {
            failureReason = "Account is not active for merchant: " + req.getMerchantId();
        } else if (account.getBalance().compareTo(req.getAmount()) < 0) {
            failureReason = "Insufficient balance on this account";
        }

        String reservationId = "RES-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        ReservationStatus status;

        if (failureReason == null) {
            account.setBalance(account.getBalance().subtract(req.getAmount()));
            accountRepository.save(account);
            status = ReservationStatus.RESERVED;
        } else {
            status = ReservationStatus.FAILED;
        }

        Reservation r = Reservation.builder()
                .reservationId(reservationId)
                .transactionId(transactionId)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .merchantId(req.getMerchantId())
                .status(status)
                .build();

        r = repository.save(r);

        if (failureReason == null) {
            log.info("Reserved: reservationId={}, transactionId={}, amount={}, currency={}, newBalance={}",
                    r.getReservationId(), r.getTransactionId(), r.getAmount(), r.getCurrency(), account.getBalance());
        } else {
            log.warn("Reservation failed: reservationId={}, transactionId={}, reason={}", r.getReservationId(), transactionId, failureReason);
        }

        return ReserveResponse.builder()
                .reservationId(r.getReservationId())
                .status(r.getStatus().name())
                .message(failureReason != null ? failureReason : "Reserved")
                .reason(failureReason)
                .build();
    }

    @Transactional
    public ReserveResponse release(String reservationId) {
        Reservation r = repository.findByReservationId(reservationId).orElse(null);
        if (r == null) {
            log.warn("Release attempted on non-existent reservation: reservationId={}", reservationId);
            return ReserveResponse.builder()
                    .reservationId(reservationId)
                    .status("FAILED")
                    .message("Reservation not found")
                    .reason("Reservation not found: " + reservationId)
                    .build();
        }

        if (r.getStatus() != ReservationStatus.RESERVED) {
            r.setReleasedAt(Instant.now());
            repository.save(r);
            log.warn("Release attempted on non-reserved reservation: reservationId={}, status={}", reservationId, r.getStatus());
            return ReserveResponse.builder()
                    .reservationId(r.getReservationId())
                    .status("FAILED")
                    .message("Reservation already released or failed")
                    .reason("Release attempted on non-reserved reservation: status=" + r.getStatus())
                    .build();
        }

        Account account = accountRepository.findByMerchantIdWithLock(r.getMerchantId()).orElse(null);
        if (account == null) {
            r.setReleasedAt(Instant.now());
            repository.save(r);
            log.warn("Account not found for reservation: reservationId={}, merchantId={}", reservationId, r.getMerchantId());
            return ReserveResponse.builder()
                    .reservationId(r.getReservationId())
                    .status("FAILED")
                    .message("Account not found")
                    .reason("Account not found for merchant: " + r.getMerchantId())
                    .build();
        }

        // Re-check reservation status within the locked context to prevent
        // a TOCTOU race: another thread may have released this reservation
        // between our initial status check and acquiring the account lock
        r = repository.findByReservationId(reservationId).orElse(null);
        if (r == null || r.getStatus() != ReservationStatus.RESERVED) {
            if (r != null) {
                r.setReleasedAt(Instant.now());
                repository.save(r);
            }
            log.warn("Release race detected – reservation no longer RESERVED: reservationId={}", reservationId);
            return ReserveResponse.builder()
                    .reservationId(reservationId)
                    .status("FAILED")
                    .message("Reservation already released or failed")
                    .reason("Release attempted on non-reserved reservation: status=" + (r != null ? r.getStatus() : "UNKNOWN"))
                    .build();
        }

        account.setBalance(account.getBalance().add(r.getAmount()));
        accountRepository.save(account);

        r.setStatus(ReservationStatus.RELEASED);
        r.setReleasedAt(Instant.now());
        repository.save(r);

        log.info("Released: reservationId={}, transactionId={}, amount={}, newBalance={}",
                reservationId, r.getTransactionId(), r.getAmount(), account.getBalance());

        return ReserveResponse.builder()
                .reservationId(r.getReservationId())
                .status(r.getStatus().name())
                .message("Released")
                .build();
    }
}

