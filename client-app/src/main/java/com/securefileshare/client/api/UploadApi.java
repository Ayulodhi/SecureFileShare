package com.securefileshare.client.api;

import com.securefileshare.client.model.EncryptedPackage;
import com.securefileshare.client.model.ServerVerdict;

import java.util.function.BiConsumer;

/**
 * Service API interface for uploading encrypted packages to object storage and obtaining server verdicts.
 * WHAT: Streams ciphertext bytes with cryptographic headers and receives the server verification verdict.
 * WHY: Decouples the upload controller from backend network implementation (Stub vs live HttpClient).
 */
public interface UploadApi {

    /**
     * Uploads an encrypted package and queries the final server verdict (Step 5 & Step 6).
     *
     * @param encryptedPackage Encrypted payload and metadata
     * @param progressConsumer Callback reporting (bytesUploaded, totalBytes)
     * @return ServerVerdict (CLEAN, SUSPICIOUS, or INFECTED)
     * @throws Exception on network error or cancellation
     */
    ServerVerdict uploadPackage(EncryptedPackage encryptedPackage, BiConsumer<Long, Long> progressConsumer) throws Exception;
}
