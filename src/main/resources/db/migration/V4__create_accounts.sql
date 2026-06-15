CREATE TABLE accounts (
    id UNIQUEIDENTIFIER PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    card_number VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);

CREATE UNIQUE INDEX idx_account_id ON accounts(account_id);
CREATE UNIQUE INDEX idx_merchant_id ON accounts(merchant_id);
