package com.securefileshare.share.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the SHARES database table.
 * WHAT: Persists recipient-bound, expiring, and revocable file share links.
 * WHY: Enables secure zero-knowledge sharing tracking across authenticated users.
 */
@Entity
@Table(name = "shares")
public class ShareRecord {

    @Id
    private String id;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "recipient_username", nullable = false)
    private String recipientUsername;

    @Column(name = "share_token", nullable = false, unique = true)
    private String shareToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE, REVOKED, EXPIRED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ShareRecord() {
        this.createdAt = Instant.now();
        this.status = "ACTIVE";
    }

    public ShareRecord(String id, UUID fileId, UUID ownerId, String fileName, String recipientUsername, String shareToken, Instant expiresAt) {
        this.id = id;
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.recipientUsername = recipientUsername;
        this.shareToken = shareToken;
        this.expiresAt = expiresAt;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getFileId() { return fileId; }
    public void setFileId(UUID fileId) { this.fileId = fileId; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
