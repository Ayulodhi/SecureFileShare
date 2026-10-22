package com.securefileshare.client.model;

import java.io.File;

/**
 * Container holding an incoming encrypted package and its security attributes.
 * WHAT: Encapsulates temporary ciphertext, temporary decrypted plaintext, IV, digital signature,
 *       expected SHA-256 digest, recipient wrapped key, and revocation/expiry status.
 * WHY: Architecture rule: Temporary plaintext files are tracked and deleted immediately
 *      if any security check fails or when the user discards the file.
 */
public class ReceivedPackage {

    private final String shareId;
    private final String fileName;
    private final long fileSizeBytes;
    private final String senderUsername;
    private final String targetRecipient;
    private final File tempCiphertextFile;
    private File tempDecryptedFile;
    private final String ivBase64;
    private final String wrappedKeyBase64;
    private final String ecdsaSignatureBase64;
    private final String expectedSha256Hex;
    private final boolean expired;
    private final boolean revoked;
    private final boolean corruptedSignature;
    private final boolean corruptedHash;
    private final boolean infected;

    public ReceivedPackage(String shareId, String fileName, long fileSizeBytes,
                           String senderUsername, String targetRecipient,
                           File tempCiphertextFile, String ivBase64, String wrappedKeyBase64,
                           String ecdsaSignatureBase64, String expectedSha256Hex,
                           boolean expired, boolean revoked,
                           boolean corruptedSignature, boolean corruptedHash, boolean infected) {
        this.shareId = shareId;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.senderUsername = senderUsername;
        this.targetRecipient = targetRecipient;
        this.tempCiphertextFile = tempCiphertextFile;
        this.ivBase64 = ivBase64;
        this.wrappedKeyBase64 = wrappedKeyBase64;
        this.ecdsaSignatureBase64 = ecdsaSignatureBase64;
        this.expectedSha256Hex = expectedSha256Hex;
        this.expired = expired;
        this.revoked = revoked;
        this.corruptedSignature = corruptedSignature;
        this.corruptedHash = corruptedHash;
        this.infected = infected;
    }

    public String getShareId() {
        return shareId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getTargetRecipient() {
        return targetRecipient;
    }

    public File getTempCiphertextFile() {
        return tempCiphertextFile;
    }

    public File getTempDecryptedFile() {
        return tempDecryptedFile;
    }

    public void setTempDecryptedFile(File tempDecryptedFile) {
        this.tempDecryptedFile = tempDecryptedFile;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public String getWrappedKeyBase64() {
        return wrappedKeyBase64;
    }

    public String getEcdsaSignatureBase64() {
        return ecdsaSignatureBase64;
    }

    public String getExpectedSha256Hex() {
        return expectedSha256Hex;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isCorruptedSignature() {
        return corruptedSignature;
    }

    public boolean isCorruptedHash() {
        return corruptedHash;
    }

    public boolean isInfected() {
        return infected;
    }

    /**
     * Purges all temporary ciphertext and decrypted files from disk.
     * Why: Prevents unverified or rejected plaintext from lingering in OS temp directories.
     */
    public void cleanup() {
        if (tempCiphertextFile != null && tempCiphertextFile.exists()) {
            tempCiphertextFile.delete();
        }
        if (tempDecryptedFile != null && tempDecryptedFile.exists()) {
            tempDecryptedFile.delete();
            tempDecryptedFile = null;
        }
    }
}
