package com.securefileshare.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object returned when retrieving or registering public keys.
 * WHAT: Conveys public keys, computed SHA-256 fingerprints, creation timestamp, and revocation flag.
 * WHY: Allows recipients/senders to verify public key fingerprints out-of-band to prevent server key-swapping attacks.
 */
public class KeyRegistryResponse {

    private UUID id;
    private UUID userId;
    private String rsaPublicKey;
    private String ecPublicKey;
    private String rsaFingerprint;
    private String ecFingerprint;
    private Instant createdAt;
    private boolean revoked;

    public KeyRegistryResponse() {}

    public KeyRegistryResponse(UUID id, UUID userId, String rsaPublicKey, String ecPublicKey,
                               String rsaFingerprint, String ecFingerprint, Instant createdAt, boolean revoked) {
        this.id = id;
        this.userId = userId;
        this.rsaPublicKey = rsaPublicKey;
        this.ecPublicKey = ecPublicKey;
        this.rsaFingerprint = rsaFingerprint;
        this.ecFingerprint = ecFingerprint;
        this.createdAt = createdAt;
        this.revoked = revoked;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public String getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(String ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public String getRsaFingerprint() {
        return rsaFingerprint;
    }

    public void setRsaFingerprint(String rsaFingerprint) {
        this.rsaFingerprint = rsaFingerprint;
    }

    public String getEcFingerprint() {
        return ecFingerprint;
    }

    public void setEcFingerprint(String ecFingerprint) {
        this.ecFingerprint = ecFingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
