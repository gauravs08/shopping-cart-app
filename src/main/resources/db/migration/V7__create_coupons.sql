CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    discount_value DECIMAL(12, 2) NOT NULL,
    min_order_amount DECIMAL(12, 2) DEFAULT 0,
    max_discount_amount DECIMAL(12, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO coupons (code, description, discount_type, discount_value, min_order_amount, max_discount_amount, usage_limit, active, valid_from, valid_until)
VALUES
('WELCOME10', 'Welcome discount - 10% off', 'PERCENTAGE', 10.00, 20.00, 50.00, 1000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year'),
('FLAT5', 'Flat 5 EUR off on orders above 30 EUR', 'FIXED', 5.00, 30.00, NULL, 500, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '6 months');
