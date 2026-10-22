package com.securefileshare.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a user's cryptographic public keys in the KEY_REGISTRY table.
 * WHAT: Stores PEM/Base64 public keys (RSA-4096 and EC P-256), their SHA-256 fingerprints, and revocation state.
 * WHY: Enables zero-knowledge public-key encryption: senders retrieve the recipient's RSA key to wrap the file key,
 *      and verify the sender's EC key for ECDSA signature validation. Server NEVER holds private keys.
 */
@Entity
@Table(name = "key_registry")
public class KeyRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rsa_public_key", nullable = false, columnDefinition = "TEXT")
    private String rsaPublicKey;

    @Column(name = "ec_public_key", nullable = false, columnDefinition = "TEXT")
    private String ecPublicKey;

    @Column(name = "rsa_fingerprint", nullable = false, length = 64)
    private String rsaFingerprint;

    @Column(name = "ec_fingerprint", nullable = false, length = 64)
    private String ecFingerprint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked;

    public KeyRegistry() {
        this.createdAt = Instant.now();
        this.revoked = false;
    }

    public KeyRegistry(User user, String rsaPublicKey, String ecPublicKey, String rsaFingerprint, String ecFingerprint) {
        this.user = user;
        this.rsaPublicKey = rsaPublicKey;
        this.ecPublicKey = ecPublicKey;
        this.rsaFingerprint = rsaFingerprint;
        this.ecFingerprint = ecFingerprint;
        this.createdAt = Instant.now();
        this.revoked = false;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
