package com.example.LedgerCore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateAccountsRequest {

    @NotEmpty(message = "At least one account must be provided")
    private List<@Valid CreateAccountRequest> accounts;
}
