package com.example.LedgerCore.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_merchant_id", columnList = "merchant_id")
})
public class Account {

    @Id
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private String accountId;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "card_number")
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.updatedAt == null) this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
