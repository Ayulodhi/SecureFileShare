package com.securefileshare.client.model;

/**
 * Data Transfer Object containing client-side cryptographic key information.
 * WHAT: Conveys public key SHA-256 fingerprints, creation metadata, and keystore state.
 * WHY: Enables users to perform out-of-band fingerprint verification without exposing private keys.
 */
public class KeyDetailsDto {

    private final String username;
    private final String rsaFingerprint;
    private final String ecFingerprint;
    private final String createdAt;
    private final String keystoreLocation;

    public KeyDetailsDto(String username, String rsaFingerprint, String ecFingerprint, String createdAt, String keystoreLocation) {
        this.username = username;
        this.rsaFingerprint = rsaFingerprint;
        this.ecFingerprint = ecFingerprint;
        this.createdAt = createdAt;
        this.keystoreLocation = keystoreLocation;
    }

    public String getUsername() {
        return username;
    }

    public String getRsaFingerprint() {
        return rsaFingerprint;
    }

    public String getEcFingerprint() {
        return ecFingerprint;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }
}
