-- ==============================================================================
-- Migration V1: Initial File Service Schema
-- WHAT: Creates FILES and FILE_KEYS tables with constraints and indexes.
-- WHY: Stores encrypted file metadata and per-recipient wrapped AES keys.
--      Zero-knowledge architecture: Plaintext and AES keys are NEVER stored in the database.
-- ==============================================================================

-- Table: FILES
-- WHAT: Stores encrypted file metadata, SHA-256 integrity hash, ECDSA signature, and MinIO object location.
-- WHY: Tracks lifecycle and cryptographic authenticity of zero-knowledge ciphertext objects.
CREATE TABLE IF NOT EXISTS files (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    ciphertext_sha256 VARCHAR(64) NOT NULL,
    iv VARCHAR(255) NOT NULL,
    ecdsa_signature TEXT NOT NULL,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_SCAN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index on owner_id for fast retrieval of caller's own files (GET /files)
-- Why: Accelerates metadata listing queries filtered by owner
CREATE INDEX IF NOT EXISTS idx_files_owner_id ON files(owner_id);

-- Index on status for future malware scan background workers
-- Why: Allows efficient polling of PENDING_SCAN records during Phase 5
CREATE INDEX IF NOT EXISTS idx_files_status ON files(status);

-- Table: FILE_KEYS
-- WHAT: Stores wrapped AES-256-GCM symmetric keys (one per recipient, including file owner).
-- WHY: Zero-knowledge sharing model: AES key is wrapped with the recipient's RSA-4096 public key.
--      Server cannot decrypt because it does not possess any user's RSA private key.
CREATE TABLE IF NOT EXISTS file_keys (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL,
    wrapped_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_file_keys_file_recipient UNIQUE (file_id, recipient_id)
);

-- Index on file_id for fetching all keys associated with a file
-- Why: Speeds up key lookups when deleting or managing shared access
CREATE INDEX IF NOT EXISTS idx_file_keys_file_id ON file_keys(file_id);

-- Index on recipient_id for authorization and package retrieval lookups
-- Why: Speeds up checking if a user has access to a specific file
CREATE INDEX IF NOT EXISTS idx_file_keys_recipient_id ON file_keys(recipient_id);
