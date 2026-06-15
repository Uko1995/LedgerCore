package com.example.LedgerCore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank
    private String merchantId;

    @NotBlank
    private String accountName;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal initialBalance;

    private String cardNumber;
}
