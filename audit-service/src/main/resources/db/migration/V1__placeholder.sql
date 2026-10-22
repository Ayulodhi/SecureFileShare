-- ==============================================================================
-- Initial migration placeholder for audit-service
-- WHAT: Creates schema migration baseline for Phase 0 health startup.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS audit_service_schema_marker (
    id SERIAL PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
