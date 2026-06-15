package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.model.Account;
import com.example.LedgerCore.model.AccountStatus;
import com.example.LedgerCore.model.Reservation;
import com.example.LedgerCore.model.ReservationStatus;
import com.example.LedgerCore.repository.AccountRepository;
import com.example.LedgerCore.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService")
class ReservationServiceTest {

    @Mock
    private ReservationRepository repository;

    @Mock
    private AccountRepository accountRepository;

    private ReservationService service;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    private ReserveRequest validRequest;

    private Account validAccount;

    @BeforeEach
    void setUp() {
        service = new ReservationService(repository, accountRepository);
        validRequest = ReserveRequest.builder()
                .transactionId("txn-123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .merchantId("merchant-1")
                .build();
        validAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-12345678")
                .merchantId("merchant-1")
                .accountName("Merchant One")
                .currency("USD")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("reserve()")
    class ReserveTests {

        @BeforeEach
        void setUp() {
            when(accountRepository.findByMerchantIdWithLock("merchant-1")).thenReturn(Optional.of(validAccount));
        }

        @Test
        @DisplayName("should create reservation, deduct balance, and return RESERVED")
        void shouldCreateReservationSuccessfully() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            ReserveResponse response = service.reserve(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("RESERVED");
            assertThat(response.getMessage()).isEqualTo("Reserved");
            assertThat(response.getReason()).isNull();
            assertThat(response.getReservationId()).startsWith("RES-");
            assertThat(validAccount.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
            verify(accountRepository).save(validAccount);
        }

        @Test
        @DisplayName("should persist all fields from the request correctly")
        void shouldPersistAllFields() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.prePersist();
                return r;
            });

            service.reserve(validRequest);

            verify(repository).save(reservationCaptor.capture());
            Reservation saved = reservationCaptor.getValue();

            assertThat(saved.getTransactionId()).isEqualTo("txn-123");
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getMerchantId()).isEqualTo("merchant-1");
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getReservationId()).startsWith("RES-");
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should generate reservationId matching pattern RES-XXXXXXXX")
        void shouldGenerateCorrectReservationIdFormat() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponse response = service.reserve(validRequest);

            assertThat(response.getReservationId()).matches("^RES-[0-9A-F]{8}$");
        }

        @Test
        @DisplayName("should generate a unique reservationId on each call")
        void shouldGenerateUniqueReservationId() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponse r1 = service.reserve(validRequest);
            ReserveResponse r2 = service.reserve(validRequest);

            assertThat(r1.getReservationId()).isNotEqualTo(r2.getReservationId());
        }

        @Test
        @DisplayName("should return existing reservation for duplicate transactionId")
        void shouldReturnExistingForDuplicateTransactionId() {
            final Reservation[] saved = new Reservation[1];
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> {
                saved[0] = inv.getArgument(0);
                saved[0].setId(UUID.randomUUID());
                return saved[0];
            });
            when(repository.findByTransactionId("txn-123"))
                    .thenReturn(Optional.empty())
                    .thenAnswer(inv -> Optional.of(saved[0]));

            ReserveResponse first = service.reserve(validRequest);
            ReserveResponse second = service.reserve(validRequest);

            assertThat(second.getReservationId()).isEqualTo(first.getReservationId());
            verify(repository, times(1)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("FAULT: reservationId uses only 8 hex chars – collision risk under load")
        void reservationIdHasCollisionRisk() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponse response = service.reserve(validRequest);
            String hexPart = response.getReservationId().replace("RES-", "");

            assertThat(hexPart).hasSize(8);
        }

        @Test
        @DisplayName("should accept amount with valid precision matching DB column")
        void shouldAcceptAmountWithValidPrecision() {
            BigDecimal valid = new BigDecimal("9999999999999.99");
            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-precise")
                    .amount(valid)
                    .currency("USD")
                    .merchantId("merchant-1")
                    .build();

            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            service.reserve(req);

            verify(repository).save(reservationCaptor.capture());
            assertThat(reservationCaptor.getValue().getAmount()).isEqualByComparingTo(valid);
        }

        @Test
        @DisplayName("should set createdAt automatically before persist")
        void shouldSetCreatedAtAutomatically() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.prePersist();
                return r;
            });

            Instant before = Instant.now();
            service.reserve(validRequest);
            Instant after = Instant.now();

            verify(repository).save(reservationCaptor.capture());
            assertThat(reservationCaptor.getValue().getCreatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should allow amounts with different currency codes")
        void shouldAllowDifferentCurrencies() {
            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-eur")
                    .amount(new BigDecimal("50.00"))
                    .currency("EUR")
                    .merchantId("merchant-1")
                    .build();

            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponse response = service.reserve(req);

            assertThat(response.getStatus()).isEqualTo("RESERVED");
        }

        @Test
        @DisplayName("should return FAILED with reason when merchantId has no account")
        void shouldReturnFailedWhenMerchantNotFound() {
            when(accountRepository.findByMerchantIdWithLock("unknown")).thenReturn(Optional.empty());

            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-unknown")
                    .amount(new BigDecimal("10.00"))
                    .currency("USD")
                    .merchantId("unknown")
                    .build();

            ReserveResponse response = service.reserve(req);

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("Account not found for merchant: unknown");
            assertThat(response.getReason()).contains("Account not found for merchant: unknown");
            verify(repository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("should return FAILED status when account is not ACTIVE")
        void shouldReturnFailedWhenAccountNotActive() {
            validAccount.setStatus(AccountStatus.SUSPENDED);

            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-suspended")
                    .amount(new BigDecimal("10.00"))
                    .currency("USD")
                    .merchantId("merchant-1")
                    .build();

            ReserveResponse response = service.reserve(req);

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("Account is not active");
            assertThat(response.getReason()).contains("Account is not active");
            verify(repository).save(any(Reservation.class));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("should return FAILED status when balance is insufficient")
        void shouldReturnFailedWhenInsufficientBalance() {
            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-insufficient")
                    .amount(new BigDecimal("1000.00"))
                    .currency("USD")
                    .merchantId("merchant-1")
                    .build();

            ReserveResponse response = service.reserve(req);

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("Insufficient balance");
            assertThat(response.getReason()).contains("Insufficient balance");
            verify(repository).save(any(Reservation.class));
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("FAULT: no merchantId validation – accepts any string including blank")
        void shouldAcceptAnyMerchantId() {
            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-merchant")
                    .amount(new BigDecimal("10.00"))
                    .currency("USD")
                    .merchantId("")
                    .build();

            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.findByMerchantIdWithLock("")).thenReturn(Optional.of(validAccount));

            assertThatCode(() -> service.reserve(req)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        private Reservation reservedReservation;
        private Account account;

        @BeforeEach
        void setUp() {
            account = Account.builder()
                    .id(UUID.randomUUID())
                    .accountId("ACC-12345678")
                    .merchantId("merchant-1")
                    .accountName("Merchant One")
                    .currency("USD")
                    .balance(new BigDecimal("400.00"))
                    .status(AccountStatus.ACTIVE)
                    .build();

            reservedReservation = Reservation.builder()
                    .id(UUID.randomUUID())
                    .reservationId("RES-12345678")
                    .transactionId("txn-123")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .merchantId("merchant-1")
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("should refund balance and update status to RELEASED")
        void shouldReleaseReservedReservation() {
            when(repository.findByReservationId("RES-12345678")).thenReturn(Optional.of(reservedReservation));
            when(accountRepository.findByMerchantIdWithLock("merchant-1")).thenReturn(Optional.of(account));

            ReserveResponse response = service.release("RES-12345678");

            assertThat(response.getStatus()).isEqualTo("RELEASED");
            assertThat(response.getMessage()).isEqualTo("Released");
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(reservedReservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
            assertThat(reservedReservation.getReleasedAt()).isNotNull();
            verify(accountRepository).save(account);
            verify(repository).save(reservedReservation);
        }

        @Test
        @DisplayName("should return FAILED with reason when reservation does not exist")
        void shouldReturnFailedWhenReservationNotFound() {
            when(repository.findByReservationId("RES-NONEXISTENT")).thenReturn(Optional.empty());

            ReserveResponse response = service.release("RES-NONEXISTENT");

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("Reservation not found");
            assertThat(response.getReason()).contains("Reservation not found");
        }

        @Test
        @DisplayName("should return FAILED with reason when reservation is not RESERVED")
        void shouldReturnFailedWhenNotReserved() {
            reservedReservation.setStatus(ReservationStatus.RELEASED);
            when(repository.findByReservationId("RES-12345678")).thenReturn(Optional.of(reservedReservation));

            ReserveResponse response = service.release("RES-12345678");

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("already released or failed");
            assertThat(response.getReason()).contains("non-reserved reservation");
            assertThat(reservedReservation.getReleasedAt()).isNotNull();
            verify(repository).save(reservedReservation);
        }

        @Test
        @DisplayName("should return FAILED with reason for FAILED reservations")
        void shouldReturnFailedForFailedReservations() {
            reservedReservation.setStatus(ReservationStatus.FAILED);
            when(repository.findByReservationId("RES-12345678")).thenReturn(Optional.of(reservedReservation));

            ReserveResponse response = service.release("RES-12345678");

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("already released or failed");
            assertThat(response.getReason()).contains("non-reserved reservation");
            assertThat(reservedReservation.getReleasedAt()).isNotNull();
            verify(repository).save(reservedReservation);
        }

        @Test
        @DisplayName("should roll back refund when account save fails")
        void shouldRollbackOnAccountSaveFailure() {
            when(repository.findByReservationId("RES-12345678")).thenReturn(Optional.of(reservedReservation));
            when(accountRepository.findByMerchantIdWithLock("merchant-1")).thenReturn(Optional.of(account));
            doThrow(new RuntimeException("DB error")).when(accountRepository).save(any());

            assertThatThrownBy(() -> service.release("RES-12345678"))
                    .isInstanceOf(RuntimeException.class);

            assertThat(reservedReservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        @DisplayName("should release reservation with special characters in reservationId")
        void shouldHandleSpecialCharactersInReservationId() {
            String reservationId = "RES-!@#$%^&*()";
            reservedReservation.setReservationId(reservationId);
            when(repository.findByReservationId(reservationId)).thenReturn(Optional.of(reservedReservation));
            when(accountRepository.findByMerchantIdWithLock("merchant-1")).thenReturn(Optional.of(account));

            ReserveResponse response = service.release(reservationId);

            assertThat(response.getStatus()).isEqualTo("RELEASED");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("FAULT: no input validation on reservationId – null/blank values not rejected")
        void shouldReturnFailedForInvalidReservationId(String invalidId) {
            when(repository.findByReservationId(invalidId)).thenReturn(Optional.empty());

            ReserveResponse response = service.release(invalidId);

            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getReason()).contains("Reservation not found");
        }
    }
}
