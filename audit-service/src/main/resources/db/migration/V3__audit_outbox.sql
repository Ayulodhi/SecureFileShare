-- ==============================================================================
-- Audit Service Migration V3: Outbox Table for Reliable Asynchronous Delivery
-- WHAT: Creates an outbox table to store audit events before dispatching to audit-service.
-- WHY: Guarantees at-least-once delivery with exponential backoff and idempotency.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS audit_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_outbox_status_retry ON audit_outbox(status, next_retry_at);
