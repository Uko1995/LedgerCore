package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.AccountResponse;
import com.example.LedgerCore.dto.CreateAccountRequest;
import com.example.LedgerCore.model.Account;
import com.example.LedgerCore.model.AccountStatus;
import com.example.LedgerCore.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    private AccountService service;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private CreateAccountRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new AccountService(repository);
        validRequest = CreateAccountRequest.builder()
                .merchantId("merchant-1")
                .accountName("Merchant One")
                .currency("USD")
                .initialBalance(new BigDecimal("1000.00"))
                .cardNumber("4111111111111111")
                .build();
    }

    private Account anAccount(String merchantId) {
        return Account.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-12345678")
                .merchantId(merchantId)
                .accountName("Merchant " + merchantId)
                .currency("USD")
                .balance(new BigDecimal("1000.00"))
                .cardNumber("4111111111111111")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create account with ACTIVE status")
        void shouldCreateActiveAccount() {
            when(repository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.prePersist();
                return a;
            });

            AccountResponse response = service.create(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            assertThat(response.getMerchantId()).isEqualTo("merchant-1");
            assertThat(response.getAccountName()).isEqualTo("Merchant One");
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(response.getCardNumber()).isEqualTo("************1111");
            assertThat(response.getAccountId()).startsWith("ACC-");
        }

        @Test
        @DisplayName("should persist all fields correctly")
        void shouldPersistAllFields() {
            when(repository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.prePersist();
                return a;
            });

            service.create(validRequest);

            verify(repository).save(accountCaptor.capture());
            Account saved = accountCaptor.getValue();

            assertThat(saved.getMerchantId()).isEqualTo("merchant-1");
            assertThat(saved.getAccountName()).isEqualTo("Merchant One");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(saved.getCardNumber()).isEqualTo("4111111111111111");
            assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(saved.getAccountId()).startsWith("ACC-");
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set zero balance when initialBalance is null")
        void shouldDefaultBalanceToZero() {
            CreateAccountRequest req = CreateAccountRequest.builder()
                    .merchantId("merchant-zero")
                    .accountName("Zero Balance")
                    .currency("EUR")
                    .build();

            when(repository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.prePersist();
                return a;
            });

            AccountResponse response = service.create(req);

            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should uppercase the currency code")
        void shouldUppercaseCurrency() {
            CreateAccountRequest req = CreateAccountRequest.builder()
                    .merchantId("merchant-currency")
                    .accountName("Currency Test")
                    .currency("eur")
                    .initialBalance(BigDecimal.ZERO)
                    .build();

            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(req);

            verify(repository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("should generate accountId matching pattern ACC-XXXXXXXX")
        void shouldGenerateCorrectAccountIdFormat() {
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse response = service.create(validRequest);

            assertThat(response.getAccountId()).matches("^ACC-[0-9A-F]{8}$");
        }

        @Test
        @DisplayName("should generate a unique accountId on each call")
        void shouldGenerateUniqueAccountId() {
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse r1 = service.create(validRequest);
            AccountResponse r2 = service.create(validRequest);

            assertThat(r1.getAccountId()).isNotEqualTo(r2.getAccountId());
        }
    }

    @Nested
    @DisplayName("createBatch()")
    class CreateBatchTests {

        @Test
        @DisplayName("should create multiple accounts")
        void shouldCreateMultipleAccounts() {
            when(repository.save(any(Account.class))).thenAnswer(inv -> {
                Account a = inv.getArgument(0);
                a.prePersist();
                return a;
            });

            List<CreateAccountRequest> requests = List.of(
                    validRequest,
                    CreateAccountRequest.builder()
                            .merchantId("merchant-2")
                            .accountName("Merchant Two")
                            .currency("EUR")
                            .initialBalance(new BigDecimal("500.00"))
                            .cardNumber("5555555555554444")
                            .build()
            );

            List<AccountResponse> responses = service.createBatch(requests);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getMerchantId()).isEqualTo("merchant-1");
            assertThat(responses.get(1).getMerchantId()).isEqualTo("merchant-2");
            assertThat(responses.get(1).getCardNumber()).isEqualTo("***********4444");
            verify(repository, times(2)).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("findByMerchantId()")
    class FindByMerchantIdTests {

        @Test
        @DisplayName("should return account when merchant exists")
        void shouldReturnAccount() {
            when(repository.findByMerchantId("merchant-1")).thenReturn(Optional.of(anAccount("merchant-1")));

            AccountResponse response = service.findByMerchantId("merchant-1");

            assertThat(response.getMerchantId()).isEqualTo("merchant-1");
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when merchant does not exist")
        void shouldThrowWhenNotFound() {
            when(repository.findByMerchantId("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByMerchantId("unknown"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Account not found for merchant: unknown");
        }
    }

    @Nested
    @DisplayName("findByAccountId()")
    class FindByAccountIdTests {

        @Test
        @DisplayName("should return account when accountId exists")
        void shouldReturnAccount() {
            when(repository.findByAccountId("ACC-12345678")).thenReturn(Optional.of(anAccount("merchant-1")));

            AccountResponse response = service.findByAccountId("ACC-12345678");

            assertThat(response.getAccountId()).isEqualTo("ACC-12345678");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when accountId does not exist")
        void shouldThrowWhenNotFound() {
            when(repository.findByAccountId("ACC-UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByAccountId("ACC-UNKNOWN"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Account not found: ACC-UNKNOWN");
        }
    }

    @Nested
    @DisplayName("listAll()")
    class ListAllTests {

        @Test
        @DisplayName("should return all accounts")
        void shouldReturnAllAccounts() {
            when(repository.findAll()).thenReturn(List.of(
                    anAccount("merchant-1"),
                    anAccount("merchant-2")
            ));

            List<AccountResponse> accounts = service.listAll();

            assertThat(accounts).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no accounts exist")
        void shouldReturnEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            List<AccountResponse> accounts = service.listAll();

            assertThat(accounts).isEmpty();
        }
    }

    @Nested
    @DisplayName("suspendAccount()")
    class SuspendTests {

        @Test
        @DisplayName("should suspend an ACTIVE account")
        void shouldSuspendAccount() {
            Account account = anAccount("merchant-1");
            account.setStatus(AccountStatus.ACTIVE);
            when(repository.findByMerchantId("merchant-1")).thenReturn(Optional.of(account));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse response = service.suspendAccount("merchant-1");

            assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when merchant does not exist")
        void shouldThrowWhenNotFound() {
            when(repository.findByMerchantId("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.suspendAccount("unknown"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activateAccount()")
    class ActivateTests {

        @Test
        @DisplayName("should activate a SUSPENDED account")
        void shouldActivateAccount() {
            Account account = anAccount("merchant-1");
            account.setStatus(AccountStatus.SUSPENDED);
            when(repository.findByMerchantId("merchant-1")).thenReturn(Optional.of(account));
            when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse response = service.activateAccount("merchant-1");

            assertThat(response.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when merchant does not exist")
        void shouldThrowWhenNotFound() {
            when(repository.findByMerchantId("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateAccount("unknown"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
