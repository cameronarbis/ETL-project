-- initialize database schema for transaction ETL pipeline --

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users/Customers table --
CREATE TABLE IF NOT EXISTS customers (
    customer_id SERIAL PRIMARY KEY,
    user_uuid UUID UNIQUE,
    user_code VARCHAR(50) UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- merchants table
CREATE TABLE IF NOT EXISTS merchants (
    merchant_id SERIAL PRIMARY KEY,
    merchant_name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- categories table
CREATE TABLE IF NOT EXISTS categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) UNIQUE NOT NULL
);

-- locations table (flattened from nested JSON)
CREATE TABLE IF NOT EXISTS locations (
    location_id SERIAL PRIMARY KEY,
    city VARCHAR(255),
    country_code VARCHAR(10),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(city, country_code, ip_address)
);

-- main transaction table (normalized and flattened)
CREATE TABLE IF NOT EXISTS transactions (
    id SERIAL PRIMARY KEY,
    transaction_uuid UUID NOT NULL,
    customer_id INTEGER REFERENCES customers(customer_id),
    merchant_id INTEGER REFERENCES merchants(merchant_id),
    category_id INTEGER REFERENCES categories(category_id),
    location_id INTEGER REFERENCES locations(location_id),

    -- transaction details
    transaction_type VARCHAR(50) NOT NULL,
    amount_usd DECIMAL(15, 2) NOT NULL,
    original_amount DECIMAL(15, 2) NOT NULL,
    original_currency VARCHAR(10) NOT NULL,
    exchange_rate DECIMAL(10, 4) DEFAULT 1.0,

    -- status and payment
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,

    -- metadata (flattened)
    device_type VARCHAR(50),
    session_uuid UUID,

    -- additional fields
    notes TEXT,

    -- timestamps
    transaction_timestamp TIMESTAMP NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- constraints
    CONSTRAINT chk_status CHECK (status IN ('completed', 'pending', 'failed')),
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('purchase', 'refund', 'transfer', 'withdrawal', 'deposit'))
);

-- indexes - create indexes for better query performance
CREATE INDEX idx_transactions_customers ON transactions (customer_id);
CREATE INDEX idx_transactions_merchant ON transactions (merchant_id);
CREATE INDEX idx_transactions_timestamp ON transactions (transaction_timestamp);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_type ON transactions (transaction_type);

-- error log table for failed transactions
CREATE TABLE IF NOT EXISTS etl_errors (
    error_id SERIAL PRIMARY KEY,
    source_data JSONB,
    error_message TEXT,
    error_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_etl_errors_created ON etl_errors (created_at);

-- prepopulate categories -- insert common categories
INSERT INTO categories (category_name) VALUES
    ('groceries'),
    ('electronics'),
    ('dining'),
    ('entertainment'),
    ('travel'),
    ('utilities'),
    ('healthcare'),
    ('clothing'),
    ('other')
ON CONFLICT (category_name) DO NOTHING;

-- helper function to get or create customers
CREATE OR REPLACE FUNCTION get_or_create_customer(
    p_user_uuid UUID DEFAULT NULL,
    p_user_code VARCHAR DEFAULT NULL,
    p_email VARCHAR DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    v_customer_id INTEGER;
BEGIN
    SELECT customer_id INTO v_customer_id
    FROM customers
    WHERE (p_user_uuid IS NOT NULL AND user_uuid = p_user_uuid)
       OR (p_user_code IS NOT NULL AND user_code = p_user_code);

    IF v_customer_id IS NULL THEN
       INSERT INTO customers (user_uuid, user_code, email)
       VALUES (p_user_uuid, p_user_code, p_email)
       RETURNING customer_id INTO v_customer_id;
    END IF;

    RETURN v_customer_id;
END;
$$ LANGUAGE plpgsql;

-- helper function to get or create merchants
CREATE OR REPLACE FUNCTION get_or_create_merchant(
    p_merchant_name VARCHAR
) RETURNS INTEGER AS $$
DECLARE
    v_merchant_id INTEGER;
BEGIN
    SELECT merchant_id INTO v_merchant_id
    FROM merchants
    WHERE merchant_name = p_merchant_name;

    IF v_merchant_id IS NULL THEN
        INSERT INTO merchants (merchant_name)
        VALUES (p_merchant_name)
        RETURNING merchant_id INTO v_merchant_id;
    END IF;

    RETURN v_merchant_id;
END;
$$ LANGUAGE plpgsql;

-- helper function to get or create locations
CREATE OR REPLACE FUNCTION get_or_create_location(
    p_city VARCHAR,
    p_country_code VARCHAR,
    p_ip_address VARCHAR
) RETURNS INTEGER AS $$
DECLARE
    v_location_id INTEGER;
BEGIN
    SELECT location_id INTO v_location_id
    FROM locations
    WHERE city = p_city
      AND country_code = p_country_code
      AND ip_address = p_ip_address;

    IF v_location_id IS NULL THEN
        INSERT INTO locations (city, country_code, ip_address)
        VALUES (p_city, p_country_code, p_ip_address)
        RETURNING location_id INTO v_location_id;
    END IF;

    RETURN v_location_id;
END;
$$ LANGUAGE plpgsql;

-- grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO datauser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO datauser;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO datauser;
