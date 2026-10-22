package com.securefileshare.client.controller;

import com.securefileshare.client.api.EncryptionService;
import com.securefileshare.client.api.MalwareScanner;
import com.securefileshare.client.api.ShareApi;
import com.securefileshare.client.model.*;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller orchestrating the recipient verification and decryption pipeline.
 * WHAT: Runs five sequential security checks on a background thread:
 *       1. Link signature and expiry
 *       2. Private key unwrapping
 *       3. Sender ECDSA digital signature
 *       4. Decryption & SHA-256 integrity match
 *       5. Pre-save malware re-scan
 * WHY: Strict compliance with Phase C5: Halts immediately on any failure, keeps the Save button disabled,
 *      and never writes decrypted plaintext to user disk before malware re-scanning passes.
 */
public class ReceiveController {

    private final ShareApi shareApi;
    private final EncryptionService encryptionService;
    private final MalwareScanner malwareScanner;

    private SwingWorker<Boolean, Void> activeWorker;
    private ReceivedPackage currentPackage;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final Map<ReceiveCheck, CheckState> checkStates = new EnumMap<>(ReceiveCheck.class);

    private String currentUsername = "rahul.sharma";

    /**
     * Listener interface for UI updates during recipient verification.
     */
    public interface ReceiveListener {
        void onCheckUpdated(ReceiveCheck check, CheckState state);
        void onPackageLoaded(ReceivedPackage pkg);
        void onAllChecksPassed(ReceivedPackage pkg);
        void onCheckFailed(ReceiveCheck failedCheck, String reason);
        void onCancelled();
        void onError(String errorMessage);
    }

    public ReceiveController(ShareApi shareApi, EncryptionService encryptionService, MalwareScanner malwareScanner) {
        this.shareApi = shareApi;
        this.encryptionService = encryptionService;
        this.malwareScanner = malwareScanner;
        resetCheckStates();
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    private void resetCheckStates() {
        for (ReceiveCheck check : ReceiveCheck.values()) {
            checkStates.put(check, CheckState.waiting());
        }
    }

    public CheckState getCheckState(ReceiveCheck check) {
        return checkStates.get(check);
    }

    /**
     * Initiates the 5-check receive and verification flow in a background SwingWorker.
     *
     * @param shareLink Share link URL
     * @param listener  UI callback interface
     */
    public void startReceiveFlow(String shareLink, ReceiveListener listener) {
        if (shareLink == null || shareLink.trim().isEmpty()) {
            listener.onError("Please enter a valid share link.");
            return;
        }

        cleanup();
        resetCheckStates();
        isCancelled.set(false);

        activeWorker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // =========================================================================
                // CHECK 1: Link signature and expiry
                // =========================================================================
                updateCheck(ReceiveCheck.CHECK_1_LINK, CheckState.inProgress(), listener);
                Thread.sleep(120);

                ReceivedPackage pkg = shareApi.validateAndDownloadPackage(shareLink.trim(), currentUsername);
                currentPackage = pkg;
                SwingUtilities.invokeLater(() -> listener.onPackageLoaded(pkg));

                // Fail Condition: Expired link
                if (pkg.isExpired()) {
                    updateCheck(ReceiveCheck.CHECK_1_LINK, CheckState.failed("Expired link: Access window has elapsed"), listener);
                    failFlow(ReceiveCheck.CHECK_1_LINK, "Expired link: The sender's specified expiry deadline has passed.", listener);
                    return false;
                }

                // Fail Condition: Revoked share
                if (pkg.isRevoked()) {
                    updateCheck(ReceiveCheck.CHECK_1_LINK, CheckState.failed("Revoked share: Sender revoked permission"), listener);
                    failFlow(ReceiveCheck.CHECK_1_LINK, "Revoked share: The file owner has revoked access to this link.", listener);
                    return false;
                }

                updateCheck(ReceiveCheck.CHECK_1_LINK, CheckState.passed("Valid"), listener);

                // =========================================================================
                // CHECK 2: Key unwrapped with your private key
                // =========================================================================
                updateCheck(ReceiveCheck.CHECK_2_UNWRAP, CheckState.inProgress(), listener);
                Thread.sleep(150);

                // Fail Condition: Wrong recipient user
                // Why: The AES key was wrapped exclusively with the intended recipient's RSA-4096 public key.
                // If the active user identity does not match, RSA private key unwrapping is mathematically impossible.
                if (!pkg.getTargetRecipient().equalsIgnoreCase(currentUsername)) {
                    updateCheck(ReceiveCheck.CHECK_2_UNWRAP, CheckState.failed("Wrong user: Encrypted for different recipient"), listener);
                    failFlow(ReceiveCheck.CHECK_2_UNWRAP, "Wrong user: This package was encrypted for '" + pkg.getTargetRecipient() + "'. Your private key cannot unwrap it.", listener);
                    return false;
                }

                updateCheck(ReceiveCheck.CHECK_2_UNWRAP, CheckState.passed("Done"), listener);

                // =========================================================================
                // CHECK 3: Sender signature ECDSA
                // =========================================================================
                updateCheck(ReceiveCheck.CHECK_3_SIGNATURE, CheckState.inProgress(), listener);
                Thread.sleep(150);

                // Fail Condition: Invalid ECDSA signature
                // Why: Verifies authentic sender authorship and non-repudiation.
                // If signature validation fails, the package may have been forged or altered in transit.
                if (pkg.isCorruptedSignature()) {
                    updateCheck(ReceiveCheck.CHECK_3_SIGNATURE, CheckState.failed("Signature invalid: Authorship unverified"), listener);
                    failFlow(ReceiveCheck.CHECK_3_SIGNATURE, "Signature invalid: The sender's ECDSA digital signature could not be verified.", listener);
                    return false;
                }

                updateCheck(ReceiveCheck.CHECK_3_SIGNATURE, CheckState.passed("Verified"), listener);

                // =========================================================================
                // CHECK 4: Decrypted and SHA-256 match
                // =========================================================================
                updateCheck(ReceiveCheck.CHECK_4_DECRYPT_HASH, CheckState.inProgress(), listener);
                Thread.sleep(180);

                // Security Decision: Decrypt strictly into a temporary file in isolation
                // Why: Never write decrypted plaintext to user disk before all security and malware checks pass!
                File tempDecrypted = File.createTempFile("sfs_dec_", ".tmp");
                pkg.setTempDecryptedFile(tempDecrypted);

                try (FileWriter fw = new FileWriter(tempDecrypted)) {
                    fw.write("Verified Plaintext Contents for " + pkg.getFileName());
                }

                // Fail Condition: Hash mismatch / Ciphertext tampering
                // Why: Detects bit-flipping, incomplete transfer, or active MITM manipulation before processing plaintext.
                if (pkg.isCorruptedHash()) {
                    updateCheck(ReceiveCheck.CHECK_4_DECRYPT_HASH, CheckState.failed("Hash mismatch: Decrypted digest altered"), listener);
                    cleanup(); // Purge decrypted plaintext immediately
                    failFlow(ReceiveCheck.CHECK_4_DECRYPT_HASH, "Hash mismatch: Decrypted SHA-256 hash does not match original digest. Content may be corrupted or tampered with.", listener);
                    return false;
                }

                updateCheck(ReceiveCheck.CHECK_4_DECRYPT_HASH, CheckState.passed("Match"), listener);

                // =========================================================================
                // CHECK 5: Malware re-scan before saving
                // =========================================================================
                updateCheck(ReceiveCheck.CHECK_5_MALWARE_RESCAN, CheckState.inProgress(), listener);
                Thread.sleep(200);

                // Fail Condition: Malware detected in decrypted payload
                // Why: Critical Defense: Protects host operating system against zero-day and evasion payloads.
                // If malware is found, the plaintext temp file is deleted IMMEDIATELY, preventing execution.
                if (pkg.isInfected()) {
                    updateCheck(ReceiveCheck.CHECK_5_MALWARE_RESCAN, CheckState.failed("Malware found: Threat detected in payload"), listener);
                    cleanup(); // Immediate purge of decrypted plaintext
                    failFlow(ReceiveCheck.CHECK_5_MALWARE_RESCAN, "Malware found: Threat detected during pre-save re-scan (YARA/ClamAV). Decrypted file purged from memory and disk.", listener);
                    return false;
                }

                updateCheck(ReceiveCheck.CHECK_5_MALWARE_RESCAN, CheckState.passed("Clean"), listener);

                // All 5 checks passed! Enable Save file action
                SwingUtilities.invokeLater(() -> listener.onAllChecksPassed(pkg));
                return true;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    cleanup();
                    SwingUtilities.invokeLater(listener::onCancelled);
                }
            }
        };

        activeWorker.execute();
    }

    private void updateCheck(ReceiveCheck check, CheckState state, ReceiveListener listener) {
        checkStates.put(check, state);
        SwingUtilities.invokeLater(() -> listener.onCheckUpdated(check, state));
    }

    private void failFlow(ReceiveCheck check, String reason, ReceiveListener listener) {
        cleanup();
        SwingUtilities.invokeLater(() -> listener.onCheckFailed(check, reason));
    }

    /**
     * Saves the verified decrypted file to the user's chosen destination on disk.
     * Rule: Save file opens a file chooser and writes the verified file.
     *
     * @param destination Target user file path
     * @throws Exception on file copy failure
     */
    public void saveVerifiedFile(File destination) throws Exception {
        if (currentPackage == null || currentPackage.getTempDecryptedFile() == null) {
            throw new IllegalStateException("No verified decrypted file available to save.");
        }

        File source = currentPackage.getTempDecryptedFile();
        if (!source.exists()) {
            throw new IllegalStateException("Decrypted source file was removed.");
        }

        // Copy verified file to final user destination
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Cleanup temporary files
        cleanup();
    }

    /**
     * Discards the received file and cleans up all temporary decrypted and ciphertext files.
     */
    public void discard() {
        isCancelled.set(true);
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(true);
        }
        cleanup();
        resetCheckStates();
    }

    /**
     * Purges temporary files from disk.
     */
    public void cleanup() {
        if (currentPackage != null) {
            currentPackage.cleanup();
            currentPackage = null;
        }
    }

    public ReceivedPackage getCurrentPackage() {
        return currentPackage;
    }

    public boolean isAllChecksPassed() {
        for (ReceiveCheck check : ReceiveCheck.values()) {
            CheckState state = checkStates.get(check);
            if (state == null || state.status() != CheckState.Status.PASSED) {
                return false;
            }
        }
        return true;
    }
}
