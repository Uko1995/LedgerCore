DROP INDEX idx_transaction_id ON reservations;
CREATE UNIQUE INDEX idx_transaction_id ON reservations(transaction_id);
