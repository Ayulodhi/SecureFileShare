package com.securefileshare.share.dto;

import com.securefileshare.share.entity.ShareRecord;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing share link details returned to clients.
 * WHAT: Exposes share ID, file details, share link URL, expiration, and status.
 * WHY: Conforms precisely to web-app ShareRecord interface contract.
 */
public class ShareResponse {

    private String id;
    private UUID fileId;
    private String fileName;
    private String recipientUsername;
    private String shareLink;
    private Instant expiresAt;
    private String status;
    private Instant createdAt;

    public ShareResponse() {}

    public ShareResponse(String id, UUID fileId, String fileName, String recipientUsername, String shareLink, Instant expiresAt, String status, Instant createdAt) {
        this.id = id;
        this.fileId = fileId;
        this.fileName = fileName;
        this.recipientUsername = recipientUsername;
        this.shareLink = shareLink;
        this.expiresAt = expiresAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ShareResponse fromEntity(ShareRecord record, String baseUrl) {
        String shareLink = baseUrl + "/share/" + record.getShareToken();
        return new ShareResponse(
                record.getId(),
                record.getFileId(),
                record.getFileName(),
                record.getRecipientUsername(),
                shareLink,
                record.getExpiresAt(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }

    public String getId() { return id; }
    public UUID getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getRecipientUsername() { return recipientUsername; }
    public String getShareLink() { return shareLink; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
