package com.securefileshare.client.model;

/**
 * Data Transfer Object representing an access grant created by the current user.
 * WHAT: Contains share ID, target file, recipient username, share status, and timestamps.
 * WHY: Powers the "Shares I created" management table with revocation controls.
 */
public class ShareRecordDto {

    private final String shareId;
    private final String fileId;
    private final String fileName;
    private final String recipient;
    private ShareStatus status;
    private final String createdAt;
    private final String expiresAt;

    public ShareRecordDto(String shareId, String fileId, String fileName, String recipient,
                          ShareStatus status, String createdAt, String expiresAt) {
        this.shareId = shareId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.recipient = recipient;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getShareId() {
        return shareId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRecipient() {
        return recipient;
    }

    public ShareStatus getStatus() {
        return status;
    }

    public void setStatus(ShareStatus status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean canRevoke() {
        return status == ShareStatus.ACTIVE;
    }
}
