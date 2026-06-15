package com.example.LedgerCore.repository;

import com.example.LedgerCore.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountId(String accountId);
    Optional<Account> findByMerchantId(String merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.merchantId = :merchantId")
    Optional<Account> findByMerchantIdWithLock(@Param("merchantId") String merchantId);
}
