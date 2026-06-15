package com.example.LedgerCore.controller;

import com.example.LedgerCore.dto.AccountResponse;
import com.example.LedgerCore.dto.ApiResponse;
import com.example.LedgerCore.dto.BatchCreateAccountsRequest;
import com.example.LedgerCore.dto.CreateAccountRequest;
import com.example.LedgerCore.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody CreateAccountRequest req) {
        AccountResponse resp = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Account created", resp));
    }

    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<AccountResponse>>> createBatch(@Valid @RequestBody BatchCreateAccountsRequest batchReq) {
        List<AccountResponse> responses = service.createBatch(batchReq.getAccounts());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Accounts created", responses));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<AccountResponse>>> listAll() {
        List<AccountResponse> accounts = service.listAll();
        return ResponseEntity.ok(ApiResponse.success(200, "Accounts retrieved", accounts));
    }

    @GetMapping(value = "/{merchantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AccountResponse>> findByMerchantId(@PathVariable String merchantId) {
        AccountResponse resp = service.findByMerchantId(merchantId);
        return ResponseEntity.ok(ApiResponse.success(200, "Account retrieved", resp));
    }

    @PatchMapping(value = "/{merchantId}/suspend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AccountResponse>> suspend(@PathVariable String merchantId) {
        AccountResponse resp = service.suspendAccount(merchantId);
        return ResponseEntity.ok(ApiResponse.success(200, "Account suspended", resp));
    }

    @PatchMapping(value = "/{merchantId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AccountResponse>> activate(@PathVariable String merchantId) {
        AccountResponse resp = service.activateAccount(merchantId);
        return ResponseEntity.ok(ApiResponse.success(200, "Account activated", resp));
    }
}
