package com.example.LedgerCore.controller;

import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
                    .andExpect(jsonPath("$.reservationId").value("RES-ABCD1234"))
                    .andExpect(jsonPath("$.status").value("RESERVED"))
                    .andExpect(jsonPath("$.message").value("Reserved"));
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

        @Test
        @DisplayName("should return 200 OK when release succeeds")
        void shouldReturn200() throws Exception {
            doNothing().when(service).release("RES-ABCD1234");

            mockMvc.perform(post("/release/{reservationId}", "RES-ABCD1234"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when reservation not found")
        void shouldReturn404WhenNotFound() throws Exception {
            String reservationId = "RES-NONEXISTENT";
            doThrow(new EntityNotFoundException("Reservation not found"))
                    .when(service).release(reservationId);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Reservation not found"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 409 when reservation already released")
        void shouldReturn409WhenAlreadyReleased() throws Exception {
            String reservationId = "RES-ALREADY-RELEASED";
            doThrow(new IllegalStateException("Reservation already released"))
                    .when(service).release(reservationId);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Reservation already released"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("FAULT: no @Valid/@NotBlank on path variable – empty string is not validated")
        void emptyReservationIdReturns500InsteadOf400() throws Exception {
            // FAULT: The path variable has no validation annotations.
            // An empty reservationId is forwarded to the service without rejection.
            // Since the service doesn't validate either, it reaches the repository
            // and eventually produces a confusing error.
            // The resulting stack trace is caught by the global Exception handler,
            // returning 500 instead of a meaningful 400-level error.
            mockMvc.perform(post("/release/{reservationId}", ""))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("should return 200 with special characters in reservationId")
        void shouldHandleSpecialCharacters() throws Exception {
            String reservationId = "RES-SPECIAL!@#";
            doNothing().when(service).release(reservationId);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return generic error message for unexpected exceptions")
        void shouldReturnGenericErrorMessage() throws Exception {
            String reservationId = "RES-ERROR";
            doThrow(new RuntimeException("Internal database connection failed: timeout"))
                    .when(service).release(reservationId);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Internal server error"))
                    .andExpect(jsonPath("$.status").value(500));
        }

        @Test
        @DisplayName("should release with UUID-style reservationId")
        void shouldReleaseWithUuidStyleId() throws Exception {
            String reservationId = "RES-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            doNothing().when(service).release(reservationId);

            mockMvc.perform(post("/release/{reservationId}", reservationId))
                    .andExpect(status().isOk());
        }
    }
}
