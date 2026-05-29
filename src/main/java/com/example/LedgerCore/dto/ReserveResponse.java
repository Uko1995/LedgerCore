package com.example.LedgerCore.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResponse {
    private String reservationId;
    private String status;
    private String message;
}

