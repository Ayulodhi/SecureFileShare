package com.securefileshare.client.controller;

import com.securefileshare.client.api.mock.MockShareApi;
import com.securefileshare.client.api.mock.StubEncryptionService;
import com.securefileshare.client.api.mock.StubMalwareScanner;
import com.securefileshare.client.model.CheckState;
import com.securefileshare.client.model.ReceiveCheck;
import com.securefileshare.client.model.ReceivedPackage;
import com.securefileshare.client.model.ScanDisposition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying recipient security checks and failure cases in ReceiveController.
 * WHAT: Asserts immediate halt and temp file cleanup across all Phase C5 failure conditions:
 *       expired link, revoked share, wrong recipient, signature invalid, hash mismatch, and malware found.
 * WHY: Strict compliance with Phase C5 Requirement 4: Guarantees zero unverified plaintext exposure.
 */
class ReceiveControllerTest {

    private MockShareApi shareApi;
    private StubEncryptionService encryptionService;
    private StubMalwareScanner malwareScanner;
    private ReceiveController controller;

    @BeforeEach
    void setUp() {
        shareApi = new MockShareApi();
        encryptionService = new StubEncryptionService(10);
        malwareScanner = new StubMalwareScanner(ScanDisposition.CLEAN);
        malwareScanner.setDelayMillis(10);

        controller = new ReceiveController(shareApi, encryptionService, malwareScanner);
        controller.setCurrentUsername("rahul.sharma");
    }

    @Test
    @DisplayName("All 5 checks must pass on a valid link and allow saving the verified file")
    void testSuccessfulReceiveFlow(@TempDir Path tempDir) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean allPassed = new AtomicBoolean(false);

        controller.startReceiveFlow("https://securefileshare.io/share/valid-token-123", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                allPassed.set(true);
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(allPassed.get()).isTrue();
        assertThat(controller.isAllChecksPassed()).isTrue();

        // Verify check chip states
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_1_LINK).status()).isEqualTo(CheckState.Status.PASSED);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_2_UNWRAP).status()).isEqualTo(CheckState.Status.PASSED);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_3_SIGNATURE).status()).isEqualTo(CheckState.Status.PASSED);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_4_DECRYPT_HASH).status()).isEqualTo(CheckState.Status.PASSED);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_5_MALWARE_RESCAN).status()).isEqualTo(CheckState.Status.PASSED);

        // Test saving verified file to disk (Requirement 3)
        File destFile = tempDir.resolve("saved_financial_statement.pdf").toFile();
        controller.saveVerifiedFile(destFile);

        assertThat(destFile.exists()).isTrue();
        assertThat(destFile.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Failure Case 1: Expired link must halt at Check 1 and mark status FAILED")
    void testExpiredLinkFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();
        AtomicReference<String> failureReasonRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/expired-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
                failureReasonRef.set(reason);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_1_LINK);
        assertThat(failureReasonRef.get()).containsIgnoringCase("expired");

        // Subsequent checks must remain WAITING
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_2_UNWRAP).status()).isEqualTo(CheckState.Status.WAITING);
        assertThat(controller.isAllChecksPassed()).isFalse();
    }

    @Test
    @DisplayName("Failure Case 2: Revoked share must halt at Check 1 with revoked message")
    void testRevokedShareFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/revoked-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_1_LINK);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_1_LINK).failureReason()).containsIgnoringCase("revoked");
    }

    @Test
    @DisplayName("Failure Case 3: Wrong user must halt at Check 2 and block key unwrapping")
    void testWrongUserFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/wrong-user-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_2_UNWRAP);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_2_UNWRAP).status()).isEqualTo(CheckState.Status.FAILED);
    }

    @Test
    @DisplayName("Failure Case 4: Corrupted ECDSA signature must halt at Check 3")
    void testCorruptedSignatureFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/bad-sig-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_3_SIGNATURE);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_3_SIGNATURE).status()).isEqualTo(CheckState.Status.FAILED);
    }

    @Test
    @DisplayName("Failure Case 5: Hash mismatch must halt at Check 4 and purge decrypted plaintext")
    void testHashMismatchFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/bad-hash-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_4_DECRYPT_HASH);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_4_DECRYPT_HASH).status()).isEqualTo(CheckState.Status.FAILED);

        // Security check: temp file must be cleaned up immediately
        assertThat(controller.getCurrentPackage()).isNull();
    }

    @Test
    @DisplayName("Failure Case 6: Malware found on re-scan must halt at Check 5 and delete decrypted file")
    void testMalwareReScanFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ReceiveCheck> failedCheckRef = new AtomicReference<>();

        controller.startReceiveFlow("https://securefileshare.io/share/infected-contract", new ReceiveController.ReceiveListener() {
            @Override
            public void onCheckUpdated(ReceiveCheck check, CheckState state) {}

            @Override
            public void onPackageLoaded(ReceivedPackage pkg) {}

            @Override
            public void onAllChecksPassed(ReceivedPackage pkg) {
                latch.countDown();
            }

            @Override
            public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
                failedCheckRef.set(failedCheck);
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(failedCheckRef.get()).isEqualTo(ReceiveCheck.CHECK_5_MALWARE_RESCAN);
        assertThat(controller.getCheckState(ReceiveCheck.CHECK_5_MALWARE_RESCAN).status()).isEqualTo(CheckState.Status.FAILED);

        // Security check: infected plaintext must be purged immediately
        assertThat(controller.getCurrentPackage()).isNull();
    }
}
