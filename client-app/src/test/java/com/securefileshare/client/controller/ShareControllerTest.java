package com.securefileshare.client.controller;

import com.securefileshare.client.api.mock.MockShareApi;
import com.securefileshare.client.model.ShareLinkResult;
import com.securefileshare.client.service.KnownFingerprintsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ShareController.
 * WHAT: Verifies the Create link button enabling rules and the TOFU key-changed warning detection.
 * WHY: Strict compliance with Phase C4 Requirement 5: Defends against server key substitution attacks.
 */
class ShareControllerTest {

    private MockShareApi mockShareApi;
    private KnownFingerprintsService knownFingerprintsService;
    private ShareController shareController;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        mockShareApi = new MockShareApi();
        Path storageFile = tempDir.resolve("known_fingerprints.properties");
        knownFingerprintsService = new KnownFingerprintsService(storageFile);
        shareController = new ShareController(mockShareApi, knownFingerprintsService);
    }

    @Test
    @DisplayName("Create link button must remain disabled until a recipient is chosen AND checkbox is ticked")
    void testButtonEnablingRules() {
        // Rule 1: No recipient, unchecked checkbox -> Disabled
        assertThat(shareController.isCreateLinkEnabled("", false)).isFalse();
        assertThat(shareController.isCreateLinkEnabled("   ", false)).isFalse();
        assertThat(shareController.isCreateLinkEnabled(null, false)).isFalse();

        // Rule 2: Recipient chosen, but checkbox NOT checked -> Disabled
        // Security reason: Defends against server key substitution by requiring explicit out-of-band verification
        assertThat(shareController.isCreateLinkEnabled("rahul", false)).isFalse();

        // Rule 3: Checkbox checked, but recipient empty -> Disabled
        assertThat(shareController.isCreateLinkEnabled("", true)).isFalse();
        assertThat(shareController.isCreateLinkEnabled(null, true)).isFalse();

        // Rule 4: Recipient chosen AND checkbox checked -> ENABLED
        assertThat(shareController.isCreateLinkEnabled("rahul", true)).isTrue();
        assertThat(shareController.isCreateLinkEnabled("priya", true)).isTrue();
    }

    @Test
    @DisplayName("Should detect when a recipient's public key fingerprint has changed from saved record")
    void testFingerprintChangedWarningDetection() {
        String recipient = "rahul";
        String trustedFingerprint = "A3:5F:0C:9E:21:7B:D4:88";

        // Save trusted fingerprint on this client device
        shareController.recordVerifiedFingerprint(recipient, trustedFingerprint);

        // Same fingerprint: no key change detected
        assertThat(shareController.isKeyChanged(recipient, trustedFingerprint)).isFalse();

        // Same fingerprint in lowercase: case-insensitive match, no key change
        assertThat(shareController.isKeyChanged(recipient, "a3:5f:0c:9e:21:7b:d4:88")).isFalse();

        // Injected attacker / substituted key fingerprint: KEY CHANGED WARNING TRIGGERED!
        String swappedFingerprint = "FF:99:88:77:66:55:44:33";
        assertThat(shareController.isKeyChanged(recipient, swappedFingerprint)).isTrue();
    }

    @Test
    @DisplayName("Trust on first use: new unknown recipient should not trigger key changed warning")
    void testTrustOnFirstUse() {
        String novelRecipient = "aman";
        String novelFingerprint = "C7:88:21:DA:11:45:9B:EE";

        // Never before seen on this device
        assertThat(knownFingerprintsService.isKnown(novelRecipient)).isFalse();
        assertThat(shareController.isKeyChanged(novelRecipient, novelFingerprint)).isFalse();
    }

    @Test
    @DisplayName("createLink should return valid share link and persist confirmed fingerprint")
    void testCreateShareLink() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ShareLinkResult> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        shareController.createLink("file-101", "contract.pdf", "rahul", "7 days", (res, err) -> {
            resultRef.set(res);
            errorRef.set(err);
            latch.countDown();
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(errorRef.get()).isNull();

        ShareLinkResult result = resultRef.get();
        assertThat(result).isNotNull();
        assertThat(result.getShareUrl()).startsWith("https://securefileshare.io/share/");
        assertThat(result.getFormattedExpiry()).contains("7 days");

        // Confirmed fingerprint should now be stored in local storage
        String saved = shareController.getSavedFingerprint("rahul");
        assertThat(saved).isEqualTo("A3:5F:0C:9E:21:7B:D4:88");
    }
}
