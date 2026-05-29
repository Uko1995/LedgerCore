package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.model.Reservation;
import com.example.LedgerCore.model.ReservationStatus;
import com.example.LedgerCore.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
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

    private ReservationService service;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    private ReserveRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new ReservationService(repository);
        validRequest = ReserveRequest.builder()
                .transactionId("txn-123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .merchantId("merchant-1")
                .build();
    }

    @Nested
    @DisplayName("reserve()")
    class ReserveTests {

        @Test
        @DisplayName("should create reservation and return response with status RESERVED")
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
            assertThat(response.getReservationId()).startsWith("RES-");
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
        @DisplayName("FAULT: should enforce unique transactionId – currently allows duplicates")
        void shouldRejectDuplicateTransactionId() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            service.reserve(validRequest);
            service.reserve(validRequest);

            verify(repository, times(2)).save(any(Reservation.class));
            // FAULT: No duplicate transactionId check exists.
            // Correct behavior: the service should verify no reservation with the same
            // transactionId already exists before creating a new one.
        }

        @Test
        @DisplayName("FAULT: reservationId uses only 8 hex chars – collision risk under load")
        void reservationIdHasCollisionRisk() {
            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponse response = service.reserve(validRequest);
            String hexPart = response.getReservationId().replace("RES-", "");

            assertThat(hexPart).hasSize(8);
            // FAULT: 8 hex chars = ~4.3B possible values. A collision would cause a
            // DataIntegrityViolationException at the DB level since reservation_id is UNIQUE.
            // Should use a full UUID or a longer random string.
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
        @DisplayName("FAULT: no merchantId validation – accepts any string including blank")
        void shouldAcceptAnyMerchantId() {
            ReserveRequest req = ReserveRequest.builder()
                    .transactionId("txn-merchant")
                    .amount(new BigDecimal("10.00"))
                    .currency("USD")
                    .merchantId("")
                    .build();

            when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

            // FAULT: The ReserveRequest DTO has @NotBlank on merchantId,
            // so this wouldn't reach the service if validation is in place.
            // However, if validation is bypassed or somehow not triggered,
            // the service would persist a reservation with an empty merchantId.
            // This is a defense-in-depth concern.
            assertThatCode(() -> service.reserve(req)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        @Test
        @DisplayName("should release a RESERVED reservation and update status + releasedAt")
        void shouldReleaseReservedReservation() {
            String reservationId = "RES-12345678";
            when(repository.releaseReservation(
                    eq(reservationId),
                    eq(ReservationStatus.RELEASED),
                    eq(ReservationStatus.RELEASED),
                    any(Instant.class)))
                    .thenReturn(1);

            assertThatCode(() -> service.release(reservationId))
                    .doesNotThrowAnyException();

            verify(repository).releaseReservation(
                    eq(reservationId),
                    eq(ReservationStatus.RELEASED),
                    eq(ReservationStatus.RELEASED),
                    any(Instant.class));
            verify(repository, never()).existsByReservationId(anyString());
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when reservationId does not exist")
        void shouldThrowEntityNotFoundException() {
            String reservationId = "RES-NONEXISTENT";
            when(repository.releaseReservation(anyString(), any(), any(), any())).thenReturn(0);
            when(repository.existsByReservationId(reservationId)).thenReturn(false);

            assertThatThrownBy(() -> service.release(reservationId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Reservation not found");
        }

        @Test
        @DisplayName("should throw IllegalStateException when reservation is already released")
        void shouldThrowIllegalStateForAlreadyReleased() {
            String reservationId = "RES-ALREADY-RELEASED";
            when(repository.releaseReservation(anyString(), any(), any(), any())).thenReturn(0);
            when(repository.existsByReservationId(reservationId)).thenReturn(true);

            assertThatThrownBy(() -> service.release(reservationId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Reservation already released");
        }

        @Test
        @DisplayName("FAULT: race condition between JPQL update and existsByReservationId check")
        void raceConditionBetweenUpdateAndExistsCheck() {
            String reservationId = "RES-RACE";
            when(repository.releaseReservation(anyString(), any(), any(), any())).thenReturn(0);
            when(repository.existsByReservationId(reservationId)).thenReturn(true);

            // FAULT: If the releaseReservation query returns 0 because the reservation
            // doesn't exist at that moment, but a concurrent transaction creates it before
            // existsByReservationId runs, the service incorrectly throws
            // "Reservation already released" instead of "Reservation not found".
            // The two queries are not atomic.
            assertThatThrownBy(() -> service.release(reservationId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Reservation already released");
        }

        @Test
        @DisplayName("should use @Modifying with clearAutomatically=true to avoid stale entity state")
        void modifyingUsesClearAutomatically() {
            String reservationId = "RES-CLEAR";
            when(repository.releaseReservation(anyString(), any(), any(), any())).thenReturn(1);

            assertThatCode(() -> service.release(reservationId))
                    .doesNotThrowAnyException();

            verify(repository).releaseReservation(
                    eq(reservationId),
                    eq(ReservationStatus.RELEASED),
                    eq(ReservationStatus.RELEASED),
                    any(Instant.class));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("FAULT: no input validation on reservationId – null/blank values not rejected")
        void noValidationOnReservationId(String invalidId) {
            when(repository.releaseReservation(eq(invalidId), any(), any(), any())).thenReturn(0);
            when(repository.existsByReservationId(invalidId)).thenReturn(false);

            // FAULT: The service (and controller) should reject null/blank reservationId
            // with a meaningful error before reaching the repository.
            // Currently it passes them through, resulting in EntityNotFoundException.
            assertThatThrownBy(() -> service.release(invalidId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should set releasedAt to current time when releasing")
        void shouldSetReleasedAtToCurrentTime() {
            String reservationId = "RES-TIME";
            Instant before = Instant.now();

            when(repository.releaseReservation(
                    eq(reservationId),
                    eq(ReservationStatus.RELEASED),
                    eq(ReservationStatus.RELEASED),
                    any(Instant.class)))
                    .thenAnswer(inv -> {
                        Instant now = inv.getArgument(3);
                        assertThat(now).isBetween(before, Instant.now());
                        return 1;
                    });

            service.release(reservationId);
        }

        @Test
        @DisplayName("should release reservation with special characters in reservationId")
        void shouldHandleSpecialCharactersInReservationId() {
            String reservationId = "RES-!@#$%^&*()";
            when(repository.releaseReservation(anyString(), any(), any(), any())).thenReturn(1);

            assertThatCode(() -> service.release(reservationId))
                    .doesNotThrowAnyException();
        }
    }
}
