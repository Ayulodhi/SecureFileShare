package com.securefileshare.client.model;

import java.io.File;

/**
 * Encapsulates an encrypted payload prepared on the client device.
 * WHAT: Holds the temporary ciphertext file, IV, digital signature, SHA-256 hash, and recipient wrapped key.
 * WHY: Architecture rule: Temporary files are tracked so they can be deleted immediately upon completion or cancellation.
 */
public class EncryptedPackage {

    private final File tempCiphertextFile;
    private final String originalFilename;
    private final long originalSizeBytes;
    private final String ivBase64;
    private final String ecdsaSignatureBase64;
    private final String ciphertextSha256Hex;
    private final String recipientWrappedKeyBase64;

    public EncryptedPackage(File tempCiphertextFile, String originalFilename, long originalSizeBytes,
                            String ivBase64, String ecdsaSignatureBase64,
                            String ciphertextSha256Hex, String recipientWrappedKeyBase64) {
        this.tempCiphertextFile = tempCiphertextFile;
        this.originalFilename = originalFilename;
        this.originalSizeBytes = originalSizeBytes;
        this.ivBase64 = ivBase64;
        this.ecdsaSignatureBase64 = ecdsaSignatureBase64;
        this.ciphertextSha256Hex = ciphertextSha256Hex;
        this.recipientWrappedKeyBase64 = recipientWrappedKeyBase64;
    }

    public File getTempCiphertextFile() {
        return tempCiphertextFile;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public String getEcdsaSignatureBase64() {
        return ecdsaSignatureBase64;
    }

    public String getCiphertextSha256Hex() {
        return ciphertextSha256Hex;
    }

    public String getRecipientWrappedKeyBase64() {
        return recipientWrappedKeyBase64;
    }

    /**
     * Deletes the temporary ciphertext file from disk.
     * Why: Prevents encrypted intermediate artifacts from lingering in local temp storage.
     */
    public void cleanup() {
        if (tempCiphertextFile != null && tempCiphertextFile.exists()) {
            tempCiphertextFile.delete();
        }
    }
}
