CREATE TABLE reservations (
    id UNIQUEIDENTIFIER PRIMARY KEY,
    reservation_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL,
    released_at DATETIME2 NULL
);

CREATE UNIQUE INDEX idx_reservation_id ON reservations(reservation_id);
CREATE INDEX idx_transaction_id ON reservations(transaction_id);

