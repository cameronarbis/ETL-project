-- V2__add_audit_trail.sql
-- Add audit trail tables for transaction tracking

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    user_id VARCHAR(255) DEFAULT 'system',
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log (created_at);

-- Data quality issues table
CREATE TABLE IF NOT EXISTS data_quality_issues (
    issue_id BIGSERIAL PRIMARY KEY,
    transaction_uuid UUID,
    issue_type VARCHAR(100),
    severity VARCHAR(20),
    message TEXT,
    raw_data JSONB,
    resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_quality_transaction ON data_quality_issues (transaction_uuid);
CREATE INDEX idx_data_quality_created ON data_quality_issues (created_at);

-- DLQ messages table
CREATE TABLE IF NOT EXISTS dlq_messages (
    dlq_id BIGSERIAL PRIMARY KEY,
    message_key VARCHAR(255),
    original_data JSONB NOT NULL,
    error_type VARCHAR(100),
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_attempted_at TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_dlq_status ON dlq_messages (status);
CREATE INDEX idx_dlq_created ON dlq_messages (created_at);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO datauser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO datauser;
