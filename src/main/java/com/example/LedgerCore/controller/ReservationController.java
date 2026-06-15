package com.example.LedgerCore.controller;

import com.example.LedgerCore.dto.ApiResponse;
import com.example.LedgerCore.dto.ReleaseRequest;
import com.example.LedgerCore.dto.ReserveRequest;
import com.example.LedgerCore.dto.ReserveResponse;
import com.example.LedgerCore.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> home() {
        return ResponseEntity.ok(ApiResponse.success(200, "LedgerCore API is running"));
    }

    @PostMapping(path = "/reserve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReserveResponse>> reserve(@Valid @RequestBody ReserveRequest req) {
        ReserveResponse resp = service.reserve(req);
        boolean isFailed = "FAILED".equals(resp.getStatus());
        if (isFailed) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.error(201, resp.getMessage(), resp));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Reservation created", resp));
    }

    @PostMapping(path = "/release/{reservationId}")
    public ResponseEntity<ApiResponse<ReserveResponse>> release(@PathVariable String reservationId) {
        return doRelease(service.release(reservationId));
    }

    @PostMapping(path = "/release", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReserveResponse>> releaseByBody(@Valid @RequestBody ReleaseRequest req) {
        return doRelease(service.release(req.getReservationId()));
    }

    private ResponseEntity<ApiResponse<ReserveResponse>> doRelease(ReserveResponse resp) {
        boolean isFailed = "FAILED".equals(resp.getStatus());
        if (isFailed) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, resp.getMessage(), resp));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Reservation released", resp));
    }
}

