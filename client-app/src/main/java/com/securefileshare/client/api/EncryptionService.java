package com.securefileshare.client.api;

import com.securefileshare.client.model.EncryptedPackage;

import java.io.File;
import java.util.function.BiConsumer;

/**
 * Service interface for client-side zero-knowledge encryption and cryptographic wrapping.
 * WHAT: Encrypts files with AES-256-GCM, wraps the AES key using recipient RSA-4096 public key,
 *       and generates SHA-256 digests and ECDSA P-256 signatures.
 * WHY: Zero-Knowledge Rule: Plaintext and private keys never leave the local device.
 */
public interface EncryptionService {

    /**
     * Executes Step 2 (AES encryption), Step 3 (RSA key wrapping), and Step 4 (SHA-256/ECDSA signing).
     *
     * @param sourceFile        Plaintext input file
     * @param recipientUsername Target recipient whose RSA public key will wrap the AES key
     * @param progressConsumer  Callback reporting (bytesProcessed, totalBytes) for large files
     * @return EncryptedPackage containing temporary ciphertext and cryptographic envelope
     * @throws Exception on encryption failure or cancellation
     */
    EncryptedPackage encryptAndWrap(File sourceFile, String recipientUsername, BiConsumer<Long, Long> progressConsumer) throws Exception;
}
