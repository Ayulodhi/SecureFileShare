package com.securefileshare.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an invite code/link in the INVITES table.
 * WHAT: Stores random unguessable invite codes with expiry, max uses, and revocation status.
 * WHY: Enables inviting new or existing users via secure links.
 */
@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Invite() {}

    public Invite(User user, String inviteCode, Instant expiresAt, int maxUses) {
        this.user = user;
        this.inviteCode = inviteCode;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.useCount = 0;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getInviteCode() { return inviteCode; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxUses() { return maxUses; }
    public int useCount() { return useCount; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public void incrementUseCount() { this.useCount++; }
    public Instant getCreatedAt() { return createdAt; }
}
