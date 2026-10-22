package com.securefileshare.client.controller;

import com.securefileshare.client.api.EncryptionService;
import com.securefileshare.client.api.MalwareScanner;
import com.securefileshare.client.api.UploadApi;
import com.securefileshare.client.model.*;

import javax.swing.*;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller orchestrating the six-step zero-knowledge upload pipeline.
 * WHAT: Runs local malware scanning, AES-256-GCM encryption, RSA-4096 wrapping, ECDSA signing,
 *       package upload, and server verdict verification on a background SwingWorker.
 * WHY: Architecture rule: Long operations never block the Swing event thread. Cancel stops the
 *      worker immediately and cleans up temporary ciphertext files.
 */
public class UploadController {

    private final MalwareScanner malwareScanner;
    private final EncryptionService encryptionService;
    private final UploadApi uploadApi;

    private SwingWorker<ServerVerdict, Void> activeWorker;
    private EncryptedPackage currentPackage;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final Map<UploadStep, StepStatus> stepStatuses = new EnumMap<>(UploadStep.class);

    /**
     * Listener interface for UI updates during upload.
     */
    public interface UploadListener {
        void onStepChanged(UploadStep step, StepStatus status);
        void onByteProgress(long bytesProcessed, long totalBytes, String stepDescription);
        void onOverallProgress(int percent);
        void onCompleted(ServerVerdict verdict, EncryptedPackage pkg);
        void onMalwareDetected(String fileName);
        void onCancelled();
        void onError(String errorMessage);
    }

    public UploadController(MalwareScanner malwareScanner, EncryptionService encryptionService, UploadApi uploadApi) {
        this.malwareScanner = malwareScanner;
        this.encryptionService = encryptionService;
        this.uploadApi = uploadApi;
        resetStepStatuses();
    }

    private void resetStepStatuses() {
        for (UploadStep step : UploadStep.values()) {
            stepStatuses.put(step, StepStatus.WAITING);
        }
    }

    public StepStatus getStepStatus(UploadStep step) {
        return stepStatuses.get(step);
    }

    /**
     * Starts the six-step upload flow in a background SwingWorker.
     *
     * @param file              File selected by user
     * @param recipientUsername Target recipient username
     * @param listener          Progress and outcome callback
     */
    public void startUpload(File file, String recipientUsername, UploadListener listener) {
        if (file == null || !file.exists()) {
            listener.onError("Please select a valid file to upload.");
            return;
        }

        resetStepStatuses();
        isCancelled.set(false);

        long totalFileSize = file.length();

        activeWorker = new SwingWorker<>() {
            @Override
            protected ServerVerdict doInBackground() throws Exception {
                // Step 1: Local malware scan (YARA + ClamAV)
                updateStep(UploadStep.STEP_1_MALWARE_SCAN, StepStatus.IN_PROGRESS, listener);
                updateByteProgress(0, totalFileSize, "Scanning file signatures...", listener);

                ScanDisposition scanResult = malwareScanner.scanFile(file, (processed, total) -> {
                    updateByteProgress(processed, total, "Scanning with YARA + ClamAV...", listener);
                    updateOverallProgress((int) ((processed * 20) / Math.max(1, total)), listener);
                });

                if (isCancelled.get() || isCancelled()) {
                    throw new InterruptedException("Upload cancelled during malware scan");
                }

                // Check outcome: If malware found, stop at Step 1! Red alert, nothing uploaded.
                // Why: Fundamental Zero-Knowledge Safety Guarantee: Infected files are stopped before encryption or network egress
                if (scanResult == ScanDisposition.MALWARE_FOUND) {
                    updateStep(UploadStep.STEP_1_MALWARE_SCAN, StepStatus.FAILED, listener);
                    SwingUtilities.invokeLater(() -> listener.onMalwareDetected(file.getName()));
                    return null;
                }

                updateStep(UploadStep.STEP_1_MALWARE_SCAN, StepStatus.DONE, listener);
                updateOverallProgress(20, listener);

                // Step 2, 3, 4: Encrypt with AES-256-GCM, wrap key with RSA-4096, hash & sign with ECDSA
                updateStep(UploadStep.STEP_2_ENCRYPT_AES, StepStatus.IN_PROGRESS, listener);

                currentPackage = encryptionService.encryptAndWrap(file, recipientUsername, (processed, total) -> {
                    updateByteProgress(processed, total, "Encrypting with AES-256-GCM...", listener);
                    int pct = 20 + (int) ((processed * 30) / Math.max(1, total));
                    updateOverallProgress(pct, listener);
                });

                if (isCancelled.get() || isCancelled()) {
                    cleanup();
                    throw new InterruptedException("Upload cancelled during encryption");
                }

                updateStep(UploadStep.STEP_2_ENCRYPT_AES, StepStatus.DONE, listener);
                updateOverallProgress(50, listener);

                // Step 3: Wrap key with recipient RSA-4096 key
                updateStep(UploadStep.STEP_3_WRAP_KEY, StepStatus.IN_PROGRESS, listener);
                Thread.sleep(100); // Simulate fast key wrap
                updateStep(UploadStep.STEP_3_WRAP_KEY, StepStatus.DONE, listener);
                updateOverallProgress(60, listener);

                // Step 4: Hash and sign (SHA-256 + ECDSA)
                updateStep(UploadStep.STEP_4_HASH_SIGN, StepStatus.IN_PROGRESS, listener);
                Thread.sleep(100); // Simulate digital signature
                updateStep(UploadStep.STEP_4_HASH_SIGN, StepStatus.DONE, listener);
                updateOverallProgress(70, listener);

                // Step 5: Upload encrypted package
                updateStep(UploadStep.STEP_5_UPLOAD_PACKAGE, StepStatus.IN_PROGRESS, listener);

                ServerVerdict verdict = uploadApi.uploadPackage(currentPackage, (processed, total) -> {
                    updateByteProgress(processed, total, "Streaming encrypted ciphertext...", listener);
                    int pct = 70 + (int) ((processed * 20) / Math.max(1, total));
                    updateOverallProgress(pct, listener);
                });

                if (isCancelled.get() || isCancelled()) {
                    cleanup();
                    throw new InterruptedException("Upload cancelled during package transmission");
                }

                updateStep(UploadStep.STEP_5_UPLOAD_PACKAGE, StepStatus.DONE, listener);
                updateOverallProgress(90, listener);

                // Step 6: Server verdict
                updateStep(UploadStep.STEP_6_SERVER_VERDICT, StepStatus.IN_PROGRESS, listener);
                Thread.sleep(150);

                if (verdict == ServerVerdict.CLEAN || verdict == ServerVerdict.SUSPICIOUS) {
                    updateStep(UploadStep.STEP_6_SERVER_VERDICT, StepStatus.DONE, listener);
                } else {
                    updateStep(UploadStep.STEP_6_SERVER_VERDICT, StepStatus.FAILED, listener);
                }

                updateOverallProgress(100, listener);
                return verdict;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    cleanup();
                    SwingUtilities.invokeLater(listener::onCancelled);
                    return;
                }

                try {
                    ServerVerdict verdict = get();
                    if (verdict != null) {
                        SwingUtilities.invokeLater(() -> listener.onCompleted(verdict, currentPackage));
                    }
                } catch (Exception ex) {
                    cleanup();
                    if (!isCancelled.get()) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        SwingUtilities.invokeLater(() -> listener.onError(msg));
                    }
                }
            }
        };

        activeWorker.execute();
    }

    /**
     * Cleanly cancels active background work and deletes any temporary encrypted files.
     * Rule: Cancel must stop the worker cleanly and delete any temporary encrypted file.
     */
    public void cancel() {
        isCancelled.set(true);
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(true);
        }
        cleanup();
    }

    /**
     * Cleans up temporary encrypted files from disk.
     */
    public void cleanup() {
        if (currentPackage != null) {
            currentPackage.cleanup();
            currentPackage = null;
        }
    }

    public EncryptedPackage getCurrentPackage() {
        return currentPackage;
    }

    public boolean isRunning() {
        return activeWorker != null && !activeWorker.isDone();
    }

    private void updateStep(UploadStep step, StepStatus status, UploadListener listener) {
        stepStatuses.put(step, status);
        SwingUtilities.invokeLater(() -> listener.onStepChanged(step, status));
    }

    private void updateByteProgress(long processed, long total, String desc, UploadListener listener) {
        SwingUtilities.invokeLater(() -> listener.onByteProgress(processed, total, desc));
    }

    private void updateOverallProgress(int percent, UploadListener listener) {
        SwingUtilities.invokeLater(() -> listener.onOverallProgress(percent));
    }
}
