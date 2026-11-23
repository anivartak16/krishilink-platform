-- Settlement amounts are stored as exact decimals instead of floating point.
ALTER TABLE offers
    MODIFY price_per_unit DECIMAL(14, 2) NOT NULL,
    MODIFY total_amount DECIMAL(14, 2) NULL;

ALTER TABLE transactions
    MODIFY amount DECIMAL(14, 2) NULL;

ALTER TABLE payments
    MODIFY amount DECIMAL(14, 2) NULL;
