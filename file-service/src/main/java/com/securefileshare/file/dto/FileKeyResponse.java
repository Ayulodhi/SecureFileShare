package com.securefileshare.file.dto;

import com.securefileshare.file.entity.FileKey;
import java.time.Instant;
import java.util.UUID;


public class FileKeyResponse {

    private UUID id;
    private UUID fileId;
    private UUID recipientId;
    private String wrappedKey;
    private Instant createdAt;

    public FileKeyResponse() {}

    public FileKeyResponse(UUID id, UUID fileId, UUID recipientId, String wrappedKey, Instant createdAt) {
        this.id = id;
        this.fileId = fileId;
        this.recipientId = recipientId;
        this.wrappedKey = wrappedKey;
        this.createdAt = createdAt;
    }

    /**
     * Converts a FileKey entity into a FileKeyResponse DTO.
     *
     * @param key FileKey JPA entity
     * @return FileKeyResponse instance
     */
    public static FileKeyResponse fromEntity(FileKey key) {
        return new FileKeyResponse(
                key.getId(),
                key.getFileRecord().getId(),
                key.getRecipientId(),
                key.getWrappedKey(),
                key.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
