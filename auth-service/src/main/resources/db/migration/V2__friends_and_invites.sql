-- ==============================================================================
-- Auth Service Migration V2: Friend Codes, Friend Requests, Friendships, and Invites
-- WHAT: Adds friend_code to users, and creates friend_requests, friendships, and invites tables.
-- WHY: Implements the secure friends and invite link subsystem per FEATURE_PLAN.md.
-- ==============================================================================

-- 1. Add friend_code column to users table and backfill existing users
ALTER TABLE users ADD COLUMN IF NOT EXISTS friend_code VARCHAR(12);

-- Backfill existing users with a unique 8-character uppercase friend code
UPDATE users
SET friend_code = 'SFS-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || id::TEXT) FROM 1 FOR 8))
WHERE friend_code IS NULL OR friend_code = '';

ALTER TABLE users ALTER COLUMN friend_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_friend_code ON users(friend_code);

-- 2. FRIEND_REQUESTS Table
CREATE TABLE IF NOT EXISTS friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, CANCELED, BLOCKED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friend_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELED', 'BLOCKED')),
    CONSTRAINT uk_friend_request_pair UNIQUE (requester_id, addressee_id)
);

CREATE INDEX IF NOT EXISTS idx_friend_requests_addressee ON friend_requests(addressee_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_requests_requester ON friend_requests(requester_id, status);

-- 3. FRIENDSHIPS Table (canonical sorted pair)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id_1 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friendship_order CHECK (user_id_1 < user_id_2),
    CONSTRAINT uk_friendship_canonical UNIQUE (user_id_1, user_id_2)
);

CREATE INDEX IF NOT EXISTS idx_friendships_user1 ON friendships(user_id_1);
CREATE INDEX IF NOT EXISTS idx_friendships_user2 ON friendships(user_id_2);

-- 4. INVITES Table
CREATE TABLE IF NOT EXISTS invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invite_code VARCHAR(32) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    max_uses INT NOT NULL DEFAULT 1,
    use_count INT NOT NULL DEFAULT 0,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invites_code ON invites(invite_code);
CREATE INDEX IF NOT EXISTS idx_invites_user ON invites(user_id);
