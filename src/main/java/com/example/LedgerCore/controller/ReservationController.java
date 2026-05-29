package com.example.LedgerCore.controller;

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
    public String home() {
        return "LedgerCore API is running.";
    }

    @PostMapping(path = "/reserve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReserveResponse> reserve(@Valid @RequestBody ReserveRequest req) {
        ReserveResponse resp = service.reserve(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping(path = "/release/{reservationId}")
    public ResponseEntity<Void> release(@PathVariable String reservationId) {
        service.release(reservationId);
        return ResponseEntity.ok().build();
    }
}

