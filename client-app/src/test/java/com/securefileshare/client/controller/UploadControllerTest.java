package com.securefileshare.client.controller;

import com.securefileshare.client.api.mock.StubEncryptionService;
import com.securefileshare.client.api.mock.StubMalwareScanner;
import com.securefileshare.client.api.mock.StubUploadApi;
import com.securefileshare.client.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying the UploadController pipeline using stubbed services.
 * WHAT: Asserts successful 6-step zero-knowledge progression, malware halt at Step 1,
 *       clean cancellation with temp file cleanup, and SUSPICIOUS quarantine verdicts.
 * WHY: Strict compliance with Phase C3 Requirement 6.
 */
class UploadControllerTest {

    private StubMalwareScanner scanner;
    private StubEncryptionService encryptionService;
    private StubUploadApi uploadApi;
    private UploadController controller;
    private File sampleFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        scanner = new StubMalwareScanner(ScanDisposition.CLEAN);
        scanner.setDelayMillis(10); // Fast for unit tests

        encryptionService = new StubEncryptionService(10);
        uploadApi = new StubUploadApi(ServerVerdict.CLEAN);
        uploadApi.setDelayMillis(10);

        controller = new UploadController(scanner, encryptionService, uploadApi);

        // Create a test file
        sampleFile = tempDir.resolve("test-document.pdf").toFile();
        try (FileWriter writer = new FileWriter(sampleFile)) {
            writer.write("Zero-Knowledge Secure File Share Payload Verification 2026");
        }
    }

    @Test
    @DisplayName("Upload pipeline should execute all six steps to DONE with a CLEAN verdict")
    void testSuccessfulUploadFlow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ServerVerdict> receivedVerdict = new AtomicReference<>();
        AtomicReference<EncryptedPackage> receivedPackage = new AtomicReference<>();

        controller.startUpload(sampleFile, "rahul", new UploadController.UploadListener() {
            @Override
            public void onStepChanged(UploadStep step, StepStatus status) {}

            @Override
            public void onByteProgress(long bytesProcessed, long totalBytes, String desc) {}

            @Override
            public void onOverallProgress(int percent) {}

            @Override
            public void onCompleted(ServerVerdict verdict, EncryptedPackage pkg) {
                receivedVerdict.set(verdict);
                receivedPackage.set(pkg);
                latch.countDown();
            }

            @Override
            public void onMalwareDetected(String fileName) {
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(receivedVerdict.get()).isEqualTo(ServerVerdict.CLEAN);

        // Verify all 6 steps reached StepStatus.DONE
        for (UploadStep step : UploadStep.values()) {
            assertThat(controller.getStepStatus(step))
                    .as("Step %s should be DONE", step)
                    .isEqualTo(StepStatus.DONE);
        }

        // Clean up test package
        if (receivedPackage.get() != null) {
            receivedPackage.get().cleanup();
        }
    }

    @Test
    @DisplayName("Malware detection must stop at Step 1 and prevent subsequent encryption or upload")
    void testMalwareDetectedStopsAtStep1() throws Exception {
        scanner.setDisposition(ScanDisposition.MALWARE_FOUND);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean malwareReported = new AtomicBoolean(false);

        controller.startUpload(sampleFile, "rahul", new UploadController.UploadListener() {
            @Override
            public void onStepChanged(UploadStep step, StepStatus status) {}

            @Override
            public void onByteProgress(long bytesProcessed, long totalBytes, String desc) {}

            @Override
            public void onOverallProgress(int percent) {}

            @Override
            public void onCompleted(ServerVerdict verdict, EncryptedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onMalwareDetected(String fileName) {
                malwareReported.set(true);
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(malwareReported.get()).isTrue();

        // Step 1 should be FAILED
        assertThat(controller.getStepStatus(UploadStep.STEP_1_MALWARE_SCAN)).isEqualTo(StepStatus.FAILED);

        // Steps 2 through 6 must remain WAITING (never executed)
        assertThat(controller.getStepStatus(UploadStep.STEP_2_ENCRYPT_AES)).isEqualTo(StepStatus.WAITING);
        assertThat(controller.getStepStatus(UploadStep.STEP_3_WRAP_KEY)).isEqualTo(StepStatus.WAITING);
        assertThat(controller.getStepStatus(UploadStep.STEP_4_HASH_SIGN)).isEqualTo(StepStatus.WAITING);
        assertThat(controller.getStepStatus(UploadStep.STEP_5_UPLOAD_PACKAGE)).isEqualTo(StepStatus.WAITING);
        assertThat(controller.getStepStatus(UploadStep.STEP_6_SERVER_VERDICT)).isEqualTo(StepStatus.WAITING);
    }

    @Test
    @DisplayName("Cancel must stop the worker cleanly and delete any temporary encrypted files")
    void testCancelStopsWorkerAndDeletesTempFiles() throws Exception {
        // Configure slower encryption step so cancel interrupts in-flight work
        encryptionService.setDelayMillis(500);

        CountDownLatch cancelLatch = new CountDownLatch(1);
        AtomicBoolean cancelReported = new AtomicBoolean(false);

        controller.startUpload(sampleFile, "rahul", new UploadController.UploadListener() {
            @Override
            public void onStepChanged(UploadStep step, StepStatus status) {}

            @Override
            public void onByteProgress(long bytesProcessed, long totalBytes, String desc) {}

            @Override
            public void onOverallProgress(int percent) {}

            @Override
            public void onCompleted(ServerVerdict verdict, EncryptedPackage pkg) {}

            @Override
            public void onMalwareDetected(String fileName) {}

            @Override
            public void onCancelled() {
                cancelReported.set(true);
                cancelLatch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                cancelLatch.countDown();
            }
        });

        // Trigger cancel shortly after start
        Thread.sleep(50);
        controller.cancel();

        boolean finished = cancelLatch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(cancelReported.get()).isTrue();

        // Verify temporary package file is purged
        assertThat(controller.getCurrentPackage()).isNull();
    }

    @Test
    @DisplayName("Server verdict SUSPICIOUS should complete with quarantine verdict")
    void testQuarantineVerdictOutcome() throws Exception {
        uploadApi.setVerdict(ServerVerdict.SUSPICIOUS);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ServerVerdict> receivedVerdict = new AtomicReference<>();

        controller.startUpload(sampleFile, "rahul", new UploadController.UploadListener() {
            @Override
            public void onStepChanged(UploadStep step, StepStatus status) {}

            @Override
            public void onByteProgress(long bytesProcessed, long totalBytes, String desc) {}

            @Override
            public void onOverallProgress(int percent) {}

            @Override
            public void onCompleted(ServerVerdict verdict, EncryptedPackage pkg) {
                receivedVerdict.set(verdict);
                latch.countDown();
            }

            @Override
            public void onMalwareDetected(String fileName) {
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(receivedVerdict.get()).isEqualTo(ServerVerdict.SUSPICIOUS);
        assertThat(controller.getStepStatus(UploadStep.STEP_6_SERVER_VERDICT)).isEqualTo(StepStatus.DONE);
    }
}
