package com.securefileshare.client.model;

/**
 * Six sequential stages of the zero-knowledge client upload pipeline.
 * WHAT: Enumerates malware scan, AES-256-GCM encryption, RSA-4096 key wrapping,
 *       SHA-256/ECDSA signing, encrypted streaming, and server verdict verification.
 * WHY: Strict compliance with Phase C3 six-step upload progress contract.
 */
public enum UploadStep {
    STEP_1_MALWARE_SCAN(1, "Local malware scan (YARA + ClamAV)"),
    STEP_2_ENCRYPT_AES(2, "Encrypt with AES-256-GCM"),
    STEP_3_WRAP_KEY(3, "Wrap key with recipient RSA-4096 key"),
    STEP_4_HASH_SIGN(4, "Hash and sign (SHA-256 + ECDSA)"),
    STEP_5_UPLOAD_PACKAGE(5, "Upload encrypted package"),
    STEP_6_SERVER_VERDICT(6, "Server verdict");

    private final int number;
    private final String description;

    UploadStep(int number, String description) {
        this.number = number;
        this.description = description;
    }

    public int getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedLabel() {
        return number + ". " + description;
    }
}
