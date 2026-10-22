package com.securefileshare.file.dto;

import com.securefileshare.file.entity.FileRecord;
import com.securefileshare.file.entity.FileStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing file metadata.
 * WHAT: Conveys public metadata (ID, owner, filename, size, hashes, signatures, status) without ciphertext.
 * WHY: Powers GET /files metadata listing and POST /files creation response while preventing ciphertext transfer.
 */
public class FileMetadataResponse {

    private UUID id;
    private UUID ownerId;
    private String originalName;
    private long sizeBytes;
    private String ciphertextSha256;
    private String iv;
    private String ecdsaSignature;
    private FileStatus status;
    private Instant createdAt;

    public FileMetadataResponse() {}

    public FileMetadataResponse(UUID id, UUID ownerId, String originalName, long sizeBytes,
                                String ciphertextSha256, String iv, String ecdsaSignature,
                                FileStatus status, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.originalName = originalName;
        this.sizeBytes = sizeBytes;
        this.ciphertextSha256 = ciphertextSha256;
        this.iv = iv;
        this.ecdsaSignature = ecdsaSignature;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Factory constructor converting a FileRecord entity into a FileMetadataResponse DTO.
     *
     * @param record FileRecord JPA entity
     * @return FileMetadataResponse instance
     */
    public static FileMetadataResponse fromEntity(FileRecord record) {
        return new FileMetadataResponse(
                record.getId(),
                record.getOwnerId(),
                record.getOriginalName(),
                record.getSizeBytes(),
                record.getCiphertextSha256(),
                record.getIv(),
                record.getEcdsaSignature(),
                record.getStatus(),
                record.getCreatedAt()
        );
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
}
