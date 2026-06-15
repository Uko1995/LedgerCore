package com.example.LedgerCore.controller;

import com.example.LedgerCore.dto.ReleaseRequest;
import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@DisplayName("ReservationController")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService service;

    @Nested
    @DisplayName("POST /reserve")
    class ReserveEndpoint {

        private ReserveRequest validRequest() {
            return ReserveRequest.builder()
                    .transactionId("txn-123")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .merchantId("merchant-1")
                    .build();
        }

        @Test
        @DisplayName("should return 201 Created with reservation response")
        void shouldReturn201() throws Exception {
            ReserveResponse resp = ReserveResponse.builder()
                    .reservationId("RES-ABCD1234")
                    .status("RESERVED")
                    .message("Reserved")
                    .build();

            when(service.reserve(any(ReserveRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Reservation created"))
                    .andExpect(jsonPath("$.data.reservationId").value("RES-ABCD1234"))
                    .andExpect(jsonPath("$.data.status").value("RESERVED"))
                    .andExpect(jsonPath("$.data.message").value("Reserved"))
                    .andExpect(jsonPath("$.data.reason").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 201 with FAILED status, message, and reason when reservation fails")
        void shouldReturn201WithReasonWhenFailed() throws Exception {
            ReserveResponse resp = ReserveResponse.builder()
                    .reservationId("RES-FAILED")
                    .status("FAILED")
                    .message("Insufficient balance: available=100, required=500")
                    .reason("Insufficient balance: available=100, required=500")
                    .build();

            when(service.reserve(any(ReserveRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Insufficient balance: available=100, required=500"))
                    .andExpect(jsonPath("$.data.reservationId").value("RES-FAILED"))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.reason").value("Insufficient balance: available=100, required=500"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when transactionId is missing")
        void shouldReturn400WhenTransactionIdMissing() throws Exception {
            ReserveRequest req = validRequest();
            req.setTransactionId(null);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when transactionId is blank")
        void shouldReturn400WhenTransactionIdBlank() throws Exception {
            ReserveRequest req = validRequest();
            req.setTransactionId("   ");

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount is null")
        void shouldReturn400WhenAmountNull() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(null);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount is below minimum (0.01)")
        void shouldReturn400WhenAmountBelowMinimum() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(new BigDecimal("0.001"));

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount is zero")
        void shouldReturn400WhenAmountZero() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(BigDecimal.ZERO);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount is negative")
        void shouldReturn400WhenAmountNegative() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(new BigDecimal("-10.00"));

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when currency is null")
        void shouldReturn400WhenCurrencyNull() throws Exception {
            ReserveRequest req = validRequest();
            req.setCurrency(null);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when currency is blank")
        void shouldReturn400WhenCurrencyBlank() throws Exception {
            ReserveRequest req = validRequest();
            req.setCurrency("");

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when currency length is not 3")
        void shouldReturn400WhenCurrencyLengthInvalid() throws Exception {
            ReserveRequest req = validRequest();
            req.setCurrency("US");

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("FAULT: currency validation allows lowercase – should be rejected or normalized")
        void currencyValidationAllowsLowercase() throws Exception {
            ReserveRequest req = validRequest();
            req.setCurrency("usd");

            when(service.reserve(any(ReserveRequest.class))).thenReturn(
                    ReserveResponse.builder()
                            .reservationId("RES-LOWERCASE")
                            .status("RESERVED")
                            .message("Reserved")
                            .build());

            // FAULT: The DTO validation (@Size(min=3, max=3)) allows lowercase currency codes.
            // ISO 4217 currencies are uppercase. The service should either validate uppercase
            // or normalize the input with toUpperCase(). This could cause downstream issues
            // if other systems expect uppercase currency codes.
            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            verify(service).reserve(any(ReserveRequest.class));
        }

        @Test
        @DisplayName("should return 400 when merchantId is missing")
        void shouldReturn400WhenMerchantIdMissing() throws Exception {
            ReserveRequest req = validRequest();
            req.setMerchantId(null);

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when merchantId is blank")
        void shouldReturn400WhenMerchantIdBlank() throws Exception {
            ReserveRequest req = validRequest();
            req.setMerchantId("");

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 for empty request body")
        void shouldReturn400ForEmptyBody() throws Exception {
            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 for malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 415 for unsupported content type")
        void shouldReturn415ForUnsupportedContentType() throws Exception {
            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("some text"))
                    .andExpect(status().isUnsupportedMediaType());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount has more than 2 decimal places")
        void shouldReturn400WhenAmountScaleExceedsLimit() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(new BigDecimal("100.999"));

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }

        @Test
        @DisplayName("should return 400 when amount has more than 13 integer digits")
        void shouldReturn400WhenAmountIntegerExceedsLimit() throws Exception {
            ReserveRequest req = validRequest();
            req.setAmount(new BigDecimal("99999999999999.00"));

            mockMvc.perform(post("/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).reserve(any());
        }
    }

    @Nested
    @DisplayName("POST /release/{reservationId}")
    class ReleaseEndpoint {

        private ReserveResponse releaseResponse;

        @BeforeEach
        void setUp() {
            releaseResponse = ReserveResponse.builder()
                    .reservationId("RES-ABCD1234")
                    .status("RELEASED")
                    .message("Released")
                    .build();
        }

        @Test
        @DisplayName("should return 200 OK when release succeeds")
        void shouldReturn200() throws Exception {
            when(service.release("RES-ABCD1234")).thenReturn(releaseResponse);

            mockMvc.perform(post("/release/{reservationId}", "RES-ABCD1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Reservation released"))
                    .andExpect(jsonPath("$.data.status").value("RELEASED"))
                    .andExpect(jsonPath("$.data.message").value("Released"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 409 Conflict when reservation not found")
        void shouldReturnFailedWhenReservationNotFound() throws Exception {
            String reservationId = "RES-NONEXISTENT";
            ReserveResponse failedResp = ReserveResponse.builder()
                    .reservationId(reservationId)
                    .status("FAILED")
                    .message("Reservation not found")
                    .reason("Reservation not found: " + reservationId)
                    .build();
            when(service.release(reservationId)).thenReturn(failedResp);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Reservation not found"))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.reason").value("Reservation not found: RES-NONEXISTENT"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 409 Conflict when reservation already released")
        void shouldReturnFailedWhenAlreadyReleased() throws Exception {
            String reservationId = "RES-ALREADY-RELEASED";
            ReserveResponse failedResp = ReserveResponse.builder()
                    .reservationId(reservationId)
                    .status("FAILED")
                    .message("Reservation already released or failed")
                    .reason("Release attempted on non-reserved reservation: status=RELEASED")
                    .build();
            when(service.release(reservationId)).thenReturn(failedResp);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Reservation already released or failed"))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.reason").value("Release attempted on non-reserved reservation: status=RELEASED"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("FAULT: no @Valid/@NotBlank on path variable – empty string not rejected")
        void emptyReservationIdReturns409WithFailed() throws Exception {
            ReserveResponse failedResp = ReserveResponse.builder()
                    .reservationId("")
                    .status("FAILED")
                    .message("Reservation not found")
                    .reason("Reservation not found: ")
                    .build();
            when(service.release("")).thenReturn(failedResp);

            mockMvc.perform(post("/release/{reservationId}", ""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 200 with special characters in reservationId")
        void shouldHandleSpecialCharacters() throws Exception {
            String reservationId = "RES-SPECIAL!@#";
            releaseResponse.setReservationId(reservationId);
            when(service.release(reservationId)).thenReturn(releaseResponse);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return generic error message for unexpected exceptions")
        void shouldReturnGenericErrorMessage() throws Exception {
            String reservationId = "RES-ERROR";
            when(service.release(reservationId)).thenThrow(new RuntimeException("Internal database connection failed: timeout"));

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"))
                    .andExpect(jsonPath("$.status").value(500));
        }

        @Test
        @DisplayName("should release with UUID-style reservationId")
        void shouldReleaseWithUuidStyleId() throws Exception {
            String reservationId = "RES-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            releaseResponse.setReservationId(reservationId);
            when(service.release(reservationId)).thenReturn(releaseResponse);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /release (body)")
    class ReleaseByBodyEndpoint {

        @Test
        @DisplayName("should return 200 OK when release via body succeeds")
        void shouldReturn200() throws Exception {
            when(service.release("RES-BODY-1234")).thenReturn(
                    ReserveResponse.builder()
                            .reservationId("RES-BODY-1234")
                            .status("RELEASED")
                            .message("Released")
                            .build());

            mockMvc.perform(post("/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ReleaseRequest.builder().reservationId("RES-BODY-1234").build())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Reservation released"))
                    .andExpect(jsonPath("$.data.status").value("RELEASED"));
        }

        @Test
        @DisplayName("should return 409 Conflict when release via body fails")
        void shouldReturn409() throws Exception {
            when(service.release("RES-BODY-FAIL")).thenReturn(
                    ReserveResponse.builder()
                            .reservationId("RES-BODY-FAIL")
                            .status("FAILED")
                            .message("Reservation not found")
                            .reason("Reservation not found: RES-BODY-FAIL")
                            .build());

            mockMvc.perform(post("/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    ReleaseRequest.builder().reservationId("RES-BODY-FAIL").build())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.data.status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 400 when reservationId is missing in body")
        void shouldReturn400WhenMissingId() throws Exception {
            mockMvc.perform(post("/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
