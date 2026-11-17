-- ==============================================================================
-- SecureFileShare - Audit Service Migration V2
-- WHAT: Adds hash_version column and unique index on previous_hash for linear chain integrity.
-- WHY: Supports mixed V1/V2 verification and prevents concurrent branch forks.
-- ==============================================================================

ALTER TABLE audit_entries ADD COLUMN IF NOT EXISTS hash_version INT NOT NULL DEFAULT 1;

-- Unique index on previous_hash to enforce strict single-predecessor linear chaining (except genesis)
CREATE UNIQUE INDEX IF NOT EXISTS idx_audit_prev_hash_unique ON audit_entries(previous_hash) WHERE previous_hash != '0000000000000000000000000000000000000000000000000000000000000000';
