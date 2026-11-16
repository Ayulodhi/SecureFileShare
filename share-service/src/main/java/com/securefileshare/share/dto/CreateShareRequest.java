package com.securefileshare.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO for creating a new share link.
 * WHAT: Encapsulates file ID, file name, recipient username, and expiration option.
 * WHY: Validates input payload before creating cryptographic share links.
 */
public class CreateShareRequest {

    @NotNull(message = "File ID is required")
    private UUID fileId;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Recipient username is required")
    private String recipientUsername;

    @NotBlank(message = "Expiry option is required")
    private String expiryOption;

    public CreateShareRequest() {}

    public CreateShareRequest(UUID fileId, String fileName, String recipientUsername, String expiryOption) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.recipientUsername = recipientUsername;
        this.expiryOption = expiryOption;
    }

    public UUID getFileId() { return fileId; }
    public void setFileId(UUID fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }

    public String getExpiryOption() { return expiryOption; }
    public void setExpiryOption(String expiryOption) { this.expiryOption = expiryOption; }
}
