package com.example.LedgerCore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRequest {
    @NotBlank
    private String reservationId;
}
