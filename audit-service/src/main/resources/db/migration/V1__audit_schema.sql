-- ==============================================================================
-- SecureFileShare - Audit Service Database Schema Migration
-- WHAT: Creates the audit_entries table for tamper-evident, hash-chained audit trails.
-- WHY: Ensures immutable compliance logging where any row modification breaks the cryptographic hash chain.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS audit_entries (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    hash VARCHAR(64) NOT NULL,
    previous_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_entries(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_entries(action);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_entries(actor);
