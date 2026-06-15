package com.example.LedgerCore.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String accountId;
    private String merchantId;
    private String accountName;
    private String currency;
    private BigDecimal balance;
    private String cardNumber;
    private String status;
    private String createdAt;
}
