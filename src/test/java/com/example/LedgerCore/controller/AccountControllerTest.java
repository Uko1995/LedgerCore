package com.example.LedgerCore.controller;

import com.example.LedgerCore.dto.AccountResponse;
import com.example.LedgerCore.dto.CreateAccountRequest;
import com.example.LedgerCore.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@DisplayName("AccountController")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService service;

    private AccountResponse sampleResponse() {
        return AccountResponse.builder()
                .accountId("ACC-12345678")
                .merchantId("merchant-1")
                .accountName("Merchant One")
                .currency("USD")
                .balance(new BigDecimal("1000.00"))
                .cardNumber("************1111")
                .status("ACTIVE")
                .createdAt("2026-01-01T00:00:00Z")
                .build();
    }

    private CreateAccountRequest validRequest() {
        return CreateAccountRequest.builder()
                .merchantId("merchant-1")
                .accountName("Merchant One")
                .currency("USD")
                .initialBalance(new BigDecimal("1000.00"))
                .cardNumber("4111111111111111")
                .build();
    }

    @Nested
    @DisplayName("POST /accounts/create")
    class CreateEndpoint {

        @Test
        @DisplayName("should return 201 Created with account response")
        void shouldReturn201() throws Exception {
            when(service.create(any(CreateAccountRequest.class))).thenReturn(sampleResponse());

            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Account created"))
                    .andExpect(jsonPath("$.data.accountId").value("ACC-12345678"))
                    .andExpect(jsonPath("$.data.merchantId").value("merchant-1"))
                    .andExpect(jsonPath("$.data.balance").value(1000.00))
                    .andExpect(jsonPath("$.data.cardNumber").value("************1111"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when merchantId is missing")
        void shouldReturn400WhenMerchantIdMissing() throws Exception {
            CreateAccountRequest req = validRequest();
            req.setMerchantId(null);

            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 when accountName is missing")
        void shouldReturn400WhenAccountNameMissing() throws Exception {
            CreateAccountRequest req = validRequest();
            req.setAccountName(null);

            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 when currency is missing")
        void shouldReturn400WhenCurrencyMissing() throws Exception {
            CreateAccountRequest req = validRequest();
            req.setCurrency(null);

            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 when currency length is not 3")
        void shouldReturn400WhenCurrencyLengthInvalid() throws Exception {
            CreateAccountRequest req = validRequest();
            req.setCurrency("US");

            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).create(any());
        }

        @Test
        @DisplayName("should return 400 for empty request body")
        void shouldReturn400ForEmptyBody() throws Exception {
            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/accounts/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /accounts/batch")
    class BatchCreateEndpoint {

        @Test
        @DisplayName("should return 201 Created with list of accounts")
        void shouldReturn201() throws Exception {
            when(service.createBatch(anyList())).thenReturn(List.of(sampleResponse()));

            String body = """
                    {
                        "accounts": [
                            {
                                "merchantId": "merchant-1",
                                "accountName": "Merchant One",
                                "currency": "USD",
                                "initialBalance": 1000.00,
                                "cardNumber": "4111111111111111"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/accounts/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data[0].accountId").value("ACC-12345678"))
                    .andExpect(jsonPath("$.data[0].cardNumber").value("************1111"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when accounts list is empty")
        void shouldReturn400WhenEmpty() throws Exception {
            mockMvc.perform(post("/accounts/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accounts\": []}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid account in batch")
        void shouldReturn400WhenInvalidAccount() throws Exception {
            String body = """
                    {
                        "accounts": [
                            {
                                "merchantId": "",
                                "accountName": "Invalid",
                                "currency": "USD"
                            }
                        ]
                    }
                    """;

            mockMvc.perform(post("/accounts/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /accounts")
    class ListEndpoint {

        @Test
        @DisplayName("should return 200 with list of accounts")
        void shouldReturn200() throws Exception {
            when(service.listAll()).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data[0].accountId").value("ACC-12345678"))
                    .andExpect(jsonPath("$.data[0].merchantId").value("merchant-1"));
        }

        @Test
        @DisplayName("should return empty list when no accounts exist")
        void shouldReturnEmptyList() throws Exception {
            when(service.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /accounts/{merchantId}")
    class FindByMerchantIdEndpoint {

        @Test
        @DisplayName("should return 200 with account")
        void shouldReturn200() throws Exception {
            when(service.findByMerchantId("merchant-1")).thenReturn(sampleResponse());

            mockMvc.perform(get("/accounts/{merchantId}", "merchant-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.merchantId").value("merchant-1"));
        }

        @Test
        @DisplayName("should return 404 when merchant not found")
        void shouldReturn404() throws Exception {
            when(service.findByMerchantId("unknown"))
                    .thenThrow(new EntityNotFoundException("Account not found for merchant: unknown"));

            mockMvc.perform(get("/accounts/{merchantId}", "unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Account not found for merchant: unknown"));
        }
    }

    @Nested
    @DisplayName("PATCH /accounts/{merchantId}/suspend")
    class SuspendEndpoint {

        @Test
        @DisplayName("should return 200 with suspended account")
        void shouldSuspend() throws Exception {
            AccountResponse suspended = sampleResponse();
            suspended.setStatus("SUSPENDED");
            when(service.suspendAccount("merchant-1")).thenReturn(suspended);

            mockMvc.perform(patch("/accounts/{merchantId}/suspend", "merchant-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("should return 404 when merchant not found")
        void shouldReturn404() throws Exception {
            when(service.suspendAccount("unknown"))
                    .thenThrow(new EntityNotFoundException("Account not found for merchant: unknown"));

            mockMvc.perform(patch("/accounts/{merchantId}/suspend", "unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /accounts/{merchantId}/activate")
    class ActivateEndpoint {

        @Test
        @DisplayName("should return 200 with activated account")
        void shouldActivate() throws Exception {
            when(service.activateAccount("merchant-1")).thenReturn(sampleResponse());

            mockMvc.perform(patch("/accounts/{merchantId}/activate", "merchant-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 404 when merchant not found")
        void shouldReturn404() throws Exception {
            when(service.activateAccount("unknown"))
                    .thenThrow(new EntityNotFoundException("Account not found for merchant: unknown"));

            mockMvc.perform(patch("/accounts/{merchantId}/activate", "unknown"))
                    .andExpect(status().isNotFound());
        }
    }
}
