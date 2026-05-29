# ledger-mock

A mock ledger microservice for reserving and releasing funds. Built with Spring Boot 3.4 and SQL Server 2022.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Database | Microsoft SQL Server 2022 |
| Migrations | Flyway |
| Persistence | Spring Data JPA (Hibernate) |
| Build | Maven |

## Prerequisites

- [Docker Desktop](https://docs.docker.com/engine/install/) (for containerized setup)
- Java 21+ and Maven (for local development without Docker)

## Quick Start

### 1. Configure secrets

```bash
cp .env.example .env
```

Edit `.env` with your credentials. The defaults work out of the box for local development.

### 2. Start with Docker

```bash
docker compose up -d
```

The API is available at `http://localhost:8081`.

### 3. Run without Docker

Start a SQL Server instance (e.g., via Docker) and point `JDBC_DATABASE_URL` in `.env` to it.

```bash
mvn package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

> [!NOTE]
> When running the JAR directly, the `.env` file is **not** automatically loaded. You must export the variables manually or set them in your shell:
> ```bash
> export $(grep -v '^#' .env | xargs)
> java -jar target/demo-0.0.1-SNAPSHOT.jar
> ```
> Alternatively, pass them inline:
> ```bash
> java -DJDBC_DATABASE_URL="$JDBC_DATABASE_URL" -DJDBC_DATABASE_USERNAME="$JDBC_DATABASE_USERNAME" -DJDBC_DATABASE_PASSWORD="$JDBC_DATABASE_PASSWORD" -jar target/demo-0.0.1-SNAPSHOT.jar
> ```

## Environment Variables

The application reads all configuration from environment variables. No secrets are hardcoded.

| Variable | Required | Description |
|----------|----------|-------------|
| `JDBC_DATABASE_URL` | Yes | JDBC connection string (e.g., `jdbc:sqlserver://localhost:1433;databaseName=ledger_mock;encrypt=false;trustServerCertificate=true`) |
| `JDBC_DATABASE_USERNAME` | Yes | Database user (e.g., `sa`) |
| `JDBC_DATABASE_PASSWORD` | Yes | Database password |
| `MSSQL_SA_PASSWORD` | Docker only | SQL Server SA password, must match `JDBC_DATABASE_PASSWORD` |

> [!NOTE]
> When using Docker Compose, the `.env` file is automatically picked up by `docker compose`. The compose file maps `db` as the database hostname internally, while local development uses `localhost`.

## API Reference

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Reserve Funds

```bash
curl -X POST http://localhost:8081/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "tx-123",
    "amount": 10.00,
    "currency": "USD",
    "merchantId": "m-1"
  }'
```

**Response** (201 Created):
```json
{
  "reservationId": "RES-1A2B3C4D",
  "status": "RESERVED",
  "message": "Reserved"
}
```

### Release Funds

```bash
curl -X POST http://localhost:8081/release/RES-1A2B3C4D
```

**Response**: `200 OK` (empty body)

### Release Errors

| Scenario | HTTP Status | Body |
|----------|-------------|------|
| Already released | 409 Conflict | `{"status": 409, "error": "Reservation already released", "timestamp": "..."}` |
| Not found | 404 Not Found | `{"status": 404, "error": "Reservation not found", "timestamp": "..."}` |

### Reserve Request Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `transactionId` | string | Required, not blank | External transaction identifier |
| `amount` | number | Required, min 0.01, up to 13 digits (2 fraction) | Amount to reserve |
| `currency` | string | Required, exactly 3 characters | ISO 4217 currency code |
| `merchantId` | string | Required, not blank | Merchant identifier |

## Development

### Build

```bash
mvn clean package
```

### Test

```bash
mvn test
```

Tests use unit and integration patterns with mocked dependencies — no database required.

### Database Migrations

Flyway migrations are in `src/main/resources/db/migration/` and run automatically on startup.

| Migration | Description |
|-----------|-------------|
| `V1__create_reservations.sql` | Creates the `reservations` table |
| `V2__alter_timestamps.sql` | Alters timestamp columns |

### Code Structure

```
src/main/java/com/example/LedgerCore/
├── controller/
│   ├── ReservationController.java   # REST endpoints
│   └── GlobalExceptionHandler.java  # Error handling
├── dto/
│   ├── ReserveRequest.java          # Request payload
│   └── ReserveResponse.java         # Response payload
├── model/
│   ├── Reservation.java             # JPA entity
│   └── ReservationStatus.java       # Enum (RESERVED / RELEASED)
├── repository/
│   └── ReservationRepository.java   # Data access
└── service/
    └── ReservationService.java      # Business logic
```

## Project Ports

| Service | Port |
|---------|------|
| Application | 8081 |
| SQL Server | 1433 |
