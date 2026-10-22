-- ==============================================================================
-- Initial Auth Service Schema Migration
-- WHAT: Creates USERS and KEY_REGISTRY tables.
-- WHY: Stores user credentials and public cryptographic keys for zero-knowledge sharing.
-- ==============================================================================

-- Create extension for UUID generation if not already active
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- USERS Table
-- Stores credentials and assigned platform authorization role
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- KEY_REGISTRY Table
-- Stores user public keys (RSA-4096 for wrapping and EC P-256 for signature verification)
-- Historical keys are retained with revoked = true for non-repudiation and auditability
CREATE TABLE key_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rsa_public_key TEXT NOT NULL,
    ec_public_key TEXT NOT NULL,
    rsa_fingerprint VARCHAR(64) NOT NULL,
    ec_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for rapid lookup and constraint enforcement
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_key_registry_user_revoked ON key_registry(user_id, revoked);
