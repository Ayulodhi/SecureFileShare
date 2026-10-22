package com.securefileshare.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


public class AddFileKeyRequest {

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotBlank(message = "Wrapped AES key cannot be blank")
    private String wrappedKey;

    public AddFileKeyRequest() {}

    public AddFileKeyRequest(UUID recipientId, String wrappedKey) {
        this.recipientId = recipientId;
        this.wrappedKey = wrappedKey;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public void setWrappedKey(String wrappedKey) {
        this.wrappedKey = wrappedKey;
    }
}
