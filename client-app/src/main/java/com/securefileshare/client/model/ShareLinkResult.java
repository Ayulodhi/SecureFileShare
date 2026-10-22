package com.securefileshare.client.model;

import java.time.Instant;

/**
 * Result returned upon successful creation of an encrypted share grant.
 * WHAT: Encapsulates share ID, target recipient, unique access URL, and expiration instant.
 * WHY: Powers the success state of the Share dialog (read-only link field, copy button, expiry time).
 */
public class ShareLinkResult {

    private final String shareId;
    private final String fileId;
    private final String fileName;
    private final String recipientUsername;
    private final String shareUrl;
    private final String formattedExpiry;
    private final Instant expiresAt;

    public ShareLinkResult(String shareId, String fileId, String fileName, String recipientUsername,
                           String shareUrl, String formattedExpiry, Instant expiresAt) {
        this.shareId = shareId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.recipientUsername = recipientUsername;
        this.shareUrl = shareUrl;
        this.formattedExpiry = formattedExpiry;
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

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public String getFormattedExpiry() {
        return formattedExpiry;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
