# API Response Formats

## POST /reserve

### Success (balance sufficient, account active)
HTTP 201
```json
{
  "success": true,
  "status": 201,
  "message": "Reservation created",
  "data": {
    "reservationId": "RES-A1B2C3D4",
    "status": "RESERVED",
    "message": "Reserved"
  },
  "timestamp": "2026-06-04T10:00:00Z"
}
```

### Failure – insufficient balance
HTTP 201 (reservation persisted with FAILED status)
```json
{
  "success": false,
  "status": 201,
  "message": "Insufficient balance: available=50000.00, required=10000.00",
  "data": {
    "reservationId": "RES-E5F6G7H8",
    "status": "FAILED",
    "message": "Insufficient balance: available=50000.00, required=10000.00"
  },
  "timestamp": "2026-06-04T10:00:00Z"
}
```

### Failure – inactive account
HTTP 201 (reservation persisted with FAILED status)
```json
{
  "success": false,
  "status": 201,
  "message": "Account is not active for merchant: MCH-1005",
  "data": {
    "reservationId": "RES-I9J0K1L2",
    "status": "FAILED",
    "message": "Account is not active for merchant: MCH-1005"
  },
  "timestamp": "2026-06-04T10:00:00Z"
}
```

### Error – merchant not found
HTTP 404
```json
{
  "success": false,
  "status": 404,
  "message": "Account not found for merchant: MCH-9999",
  "data": null,
  "timestamp": "2026-06-04T10:00:00Z"
}
```

---

## POST /accounts/batch

### Success
HTTP 201
```json
{
  "success": true,
  "status": 201,
  "message": "Accounts created",
  "data": [
    {
      "accountId": "ACC-1A2B3C4D",
      "merchantId": "MCH-1001",
      "accountName": "Acme Global Trading",
      "currency": "NGN",
      "balance": 50000.00,
      "cardNumber": "4532015112890367",
      "status": "ACTIVE",
      "createdAt": "2026-06-04T10:00:00Z"
    }
  ],
  "timestamp": "2026-06-04T10:00:00Z"
}
```

### Error – malformed request body
HTTP 400
```json
{
  "success": false,
  "status": 400,
  "message": "Malformed request body",
  "data": null,
  "timestamp": "2026-06-04T10:00:00Z"
}
```

### Error – validation failed
HTTP 400
```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "data": null,
  "timestamp": "2026-06-04T10:00:00Z"
}
```

---

## Calling service mapping

| LedgerCore `success` | LedgerCore `data.status` | Calling service action |
|---|---|---|
| `true` | `RESERVED` | Treat as success |
| `false` | `FAILED` | Reject with `"Fund reservation rejected"` |
| `false` | `null` (404) | Reject with `"Merchant not found"` |
