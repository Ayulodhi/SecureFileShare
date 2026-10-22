package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.EncryptionService;
import com.securefileshare.client.model.EncryptedPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.function.BiConsumer;

/**
 * Stub implementation of EncryptionService for Phase C3.
 * WHAT: Simulates AES-256-GCM chunked encryption, RSA-4096 key wrapping, and ECDSA signature creation,
 *       creating a real temporary file on disk.
 * WHY: Enables testing clean background progression, byte-level tracking for large files,
 *      and verifying that temporary files are deleted upon cancellation.
 */
public class StubEncryptionService implements EncryptionService {

    private int delayMillis = 300;

    public StubEncryptionService() {}

    public StubEncryptionService(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Override
    public EncryptedPackage encryptAndWrap(File sourceFile, String recipientUsername, BiConsumer<Long, Long> progressConsumer) throws Exception {
        long totalBytes = sourceFile != null && sourceFile.exists() ? sourceFile.length() : 2 * 1024 * 1024;
        String fileName = sourceFile != null ? sourceFile.getName() : "sample.pdf";

        // Create a real temporary file in the OS temp directory
        File tempCipherfile = File.createTempFile("sfs_enc_", ".tmp");

        int steps = 6;
        long chunkSize = totalBytes / steps;

        try (FileOutputStream fos = new FileOutputStream(tempCipherfile)) {
            byte[] mockChunk = new byte[1024];
            for (int i = 1; i <= steps; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    tempCipherfile.delete();
                    throw new InterruptedException("Encryption cancelled by user");
                }

                fos.write(mockChunk);
                long currentBytes = Math.min(totalBytes, i * chunkSize);
                if (progressConsumer != null) {
                    progressConsumer.accept(currentBytes, totalBytes);
                }

                Thread.sleep(Math.max(10, delayMillis / steps));
            }
        } catch (Exception ex) {
            tempCipherfile.delete();
            throw ex;
        }

        return new EncryptedPackage(
                tempCipherfile,
                fileName,
                totalBytes,
                Base64.getEncoder().encodeToString(new byte[12]), // Mock 12-byte IV
                Base64.getEncoder().encodeToString("mock-ecdsa-p256-signature".getBytes()),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Base64.getEncoder().encodeToString(("wrapped-key-for-" + recipientUsername).getBytes())
        );
    }
}
