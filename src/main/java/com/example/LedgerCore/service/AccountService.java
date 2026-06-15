package com.example.LedgerCore.service;

import com.example.LedgerCore.dto.AccountResponse;
import com.example.LedgerCore.dto.CreateAccountRequest;
import com.example.LedgerCore.model.Account;
import com.example.LedgerCore.model.AccountStatus;
import com.example.LedgerCore.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;

    @Transactional
    public AccountResponse create(CreateAccountRequest req) {
        String accountId = "ACC-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);

        Account account = Account.builder()
                .accountId(accountId)
                .merchantId(req.getMerchantId())
                .accountName(req.getAccountName())
                .currency(req.getCurrency().toUpperCase(Locale.ROOT))
                .balance(req.getInitialBalance() != null ? req.getInitialBalance() : BigDecimal.ZERO)
                .cardNumber(req.getCardNumber())
                .status(AccountStatus.ACTIVE)
                .build();

        account = repository.save(account);

        log.info("Account created: accountId={}, merchantId={}, currency={}, balance={}",
                account.getAccountId(), account.getMerchantId(), account.getCurrency(), account.getBalance());

        return toResponse(account);
    }

    @Transactional
    public List<AccountResponse> createBatch(List<CreateAccountRequest> requests) {
        List<AccountResponse> responses = new ArrayList<>();
        for (CreateAccountRequest req : requests) {
            responses.add(create(req));
        }
        log.info("Batch created: {} accounts", responses.size());
        return responses;
    }

    @Transactional(readOnly = true)
    public AccountResponse findByMerchantId(String merchantId) {
        Account account = repository.findByMerchantId(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found for merchant: " + merchantId));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse findByAccountId(String accountId) {
        Account account = repository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse suspendAccount(String merchantId) {
        Account account = repository.findByMerchantId(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found for merchant: " + merchantId));
        account.setStatus(AccountStatus.SUSPENDED);
        account = repository.save(account);
        log.info("Account suspended: accountId={}, merchantId={}", account.getAccountId(), merchantId);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse activateAccount(String merchantId) {
        Account account = repository.findByMerchantId(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found for merchant: " + merchantId));
        account.setStatus(AccountStatus.ACTIVE);
        account = repository.save(account);
        log.info("Account activated: accountId={}, merchantId={}", account.getAccountId(), merchantId);
        return toResponse(account);
    }

    private AccountResponse toResponse(Account account) {
        String raw = account.getCardNumber();
        String masked = raw != null && raw.length() >= 4
                ? "*".repeat(raw.length() - 4) + raw.substring(raw.length() - 4)
                : raw;
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .merchantId(account.getMerchantId())
                .accountName(account.getAccountName())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .cardNumber(masked)
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt().toString())
                .build();
    }
}
