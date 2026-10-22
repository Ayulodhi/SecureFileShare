package com.securefileshare.file.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the FILE_KEYS database table.
 * WHAT: Stores an asymmetric-wrapped AES symmetric key for a specific recipient (or the file owner).
 * WHY: Enables zero-knowledge multi-recipient sharing. Each recipient decrypts the AES key using their
 *      own RSA-4096 private key on their client device. Server never sees or stores the unwrapped AES key.
 */
@Entity
@Table(name = "file_keys", uniqueConstraints = {
        @UniqueConstraint(name = "uq_file_keys_file_recipient", columnNames = {"file_id", "recipient_id"})
})
public class FileKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileRecord fileRecord;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "wrapped_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FileKey() {
        this.createdAt = Instant.now();
    }

    public FileKey(FileRecord fileRecord, UUID recipientId, String wrappedKey) {
        this.fileRecord = fileRecord;
        this.recipientId = recipientId;
        this.wrappedKey = wrappedKey;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FileRecord getFileRecord() {
        return fileRecord;
    }

    public void setFileRecord(FileRecord fileRecord) {
        this.fileRecord = fileRecord;
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
