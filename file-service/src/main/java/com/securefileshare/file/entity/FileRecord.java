package com.securefileshare.file.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping to the FILES database table.
 * WHAT: Persists encrypted file metadata, cryptographic integrity hashes, signatures, and MinIO storage paths.
 * WHY: Maintains the metadata index for zero-knowledge files without ever storing plaintext or AES encryption keys.
 */
@Entity
@Table(name = "files")
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "ciphertext_sha256", nullable = false, length = 64)
    private String ciphertextSha256;

    @Column(name = "iv", nullable = false)
    private String iv;

    @Column(name = "ecdsa_signature", nullable = false, columnDefinition = "TEXT")
    private String ecdsaSignature;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "fileRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileKey> fileKeys = new ArrayList<>();

    public FileRecord() {
        this.status = FileStatus.PENDING_SCAN;
        this.createdAt = Instant.now();
    }

    public FileRecord(UUID ownerId, String originalName, long sizeBytes, String ciphertextSha256,
                      String iv, String ecdsaSignature, String objectKey) {
        this.ownerId = ownerId;
        this.originalName = originalName;
        this.sizeBytes = sizeBytes;
        this.ciphertextSha256 = ciphertextSha256;
        this.iv = iv;
        this.ecdsaSignature = ecdsaSignature;
        this.objectKey = objectKey;
        this.status = FileStatus.PENDING_SCAN;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = FileStatus.PENDING_SCAN;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCiphertextSha256() {
        return ciphertextSha256;
    }

    public void setCiphertextSha256(String ciphertextSha256) {
        this.ciphertextSha256 = ciphertextSha256;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getEcdsaSignature() {
        return ecdsaSignature;
    }

    public void setEcdsaSignature(String ecdsaSignature) {
        this.ecdsaSignature = ecdsaSignature;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<FileKey> getFileKeys() {
        return fileKeys;
    }

    public void setFileKeys(List<FileKey> fileKeys) {
        this.fileKeys = fileKeys;
    }
}
