-- ==============================================================================
-- SecureFileShare - Share Service Database Schema Migration
-- WHAT: Creates the shares table for secure, expiring, and revocable file share links.
-- WHY: Persists share records, recipient bindings, share tokens, and expiration states.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS shares (
    id VARCHAR(64) PRIMARY KEY,
    file_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    recipient_username VARCHAR(255) NOT NULL,
    share_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shares_token ON shares(share_token);
CREATE INDEX IF NOT EXISTS idx_shares_owner ON shares(owner_id);
CREATE INDEX IF NOT EXISTS idx_shares_file ON shares(file_id);
