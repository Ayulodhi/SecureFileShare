package com.securefileshare.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for requesting a file share email notification.
 * WHAT: Encapsulates recipient email/username, sender username, and shared file name.
 * WHY: Instructs auth-service to send either a share notification (if registered) or invite email (if unregistered).
 */
public class NotifyShareRequest {

    @NotBlank(message = "Recipient identifier is required")
    private String recipientIdentifier;

    @NotBlank(message = "Sender username is required")
    private String senderUsername;

    @NotBlank(message = "File name is required")
    private String fileName;

    public NotifyShareRequest() {}

    public NotifyShareRequest(String recipientIdentifier, String senderUsername, String fileName) {
        this.recipientIdentifier = recipientIdentifier;
        this.senderUsername = senderUsername;
        this.fileName = fileName;
    }

    public String getRecipientIdentifier() { return recipientIdentifier; }
    public void setRecipientIdentifier(String recipientIdentifier) { this.recipientIdentifier = recipientIdentifier; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
