package com.securefileshare.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for registering a user's cryptographic public keys.
 * WHAT: Contains the user's RSA-4096 public key (for key wrapping) and EC P-256 public key (for signing).
 * WHY: Submits cryptographic public keys to the server for distribution to file senders.
 */
public class KeyRegistrationRequest {

    @NotBlank(message = "RSA public key is required in PEM or X.509 format")
    private String rsaPublicKey;

    @NotBlank(message = "EC public key is required in PEM or X.509 format")
    private String ecPublicKey;

    public KeyRegistrationRequest() {}

    public KeyRegistrationRequest(String rsaPublicKey, String ecPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
        this.ecPublicKey = ecPublicKey;
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
}
