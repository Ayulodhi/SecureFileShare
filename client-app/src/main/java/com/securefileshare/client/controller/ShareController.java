package com.securefileshare.client.controller;

import com.securefileshare.client.api.ShareApi;
import com.securefileshare.client.model.ShareLinkResult;
import com.securefileshare.client.service.KnownFingerprintsService;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Controller managing secure file sharing operations.
 * WHAT: Coordinates recipient public key fingerprint resolution, key-change detection against local storage,
 *       button validation rules, and share link creation.
 * WHY: Strict compliance with Phase C4: Defends against server key substitution by verifying fingerprints
 *      and persisting trusted keys in local device storage.
 */
public class ShareController {

    private final ShareApi shareApi;
    private final KnownFingerprintsService knownFingerprintsService;

    // Known sample fingerprints for mock directory lookups
    private final Map<String, String> directoryFingerprints = new HashMap<>();

    public ShareController(ShareApi shareApi, KnownFingerprintsService knownFingerprintsService) {
        this.shareApi = shareApi;
        this.knownFingerprintsService = knownFingerprintsService;
        seedDirectoryFingerprints();
    }

    private void seedDirectoryFingerprints() {
        directoryFingerprints.put("rahul", "A3:5F:0C:9E:21:7B:D4:88");
        directoryFingerprints.put("priya", "B4:12:8E:44:90:3A:FF:21");
        directoryFingerprints.put("aman", "C7:88:21:DA:11:45:9B:EE");
        directoryFingerprints.put("neha", "D9:43:55:01:A2:8F:CC:33");
        directoryFingerprints.put("dev-team", "E1:67:32:89:FA:44:11:80");
        directoryFingerprints.put("security-admin", "F0:99:A8:12:33:65:78:BC");
    }

    /**
     * Resolves the public key fingerprint for a recipient username.
     *
     * @param username Target recipient username
     * @return 8-octet colon-separated fingerprint
     */
    public String resolveRecipientFingerprint(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "—";
        }
        String normalized = username.trim().toLowerCase();
        if (directoryFingerprints.containsKey(normalized)) {
            return directoryFingerprints.get(normalized);
        }
        // Generate deterministic mock fingerprint for novel usernames
        int hash = normalized.hashCode();
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X",
                (hash >> 24) & 0xFF, (hash >> 16) & 0xFF, (hash >> 8) & 0xFF, hash & 0xFF,
                (~hash >> 24) & 0xFF, (~hash >> 16) & 0xFF, (~hash >> 8) & 0xFF, ~hash & 0xFF);
    }

    /**
     * Allows tests and simulated scenarios to inject or mutate a directory fingerprint.
     */
    public void setDirectoryFingerprint(String username, String fingerprint) {
        directoryFingerprints.put(username.trim().toLowerCase(), fingerprint);
    }

    /**
     * Checks if the incoming recipient fingerprint conflicts with previously saved local records.
     *
     * @param username            Recipient username
     * @param incomingFingerprint Resolved public key fingerprint
     * @return True if fingerprint was previously recorded and does not match incoming value
     */
    public boolean isKeyChanged(String username, String incomingFingerprint) {
        return knownFingerprintsService.hasKeyChanged(username, incomingFingerprint);
    }

    public String getSavedFingerprint(String username) {
        return knownFingerprintsService.getKnownFingerprint(username);
    }

    public void recordVerifiedFingerprint(String username, String fingerprint) {
        knownFingerprintsService.saveFingerprint(username, fingerprint);
    }

    /**
     * Evaluates whether the "Create link" action button should be enabled.
     * WHAT: Requires both a non-empty recipient AND explicit user confirmation of the fingerprint.
     * WHY: Security Decision: Defends against server key substitution.
     *      Requiring the sender to explicitly confirm the recipient's fingerprint before key wrapping
     *      ensures the sender has verified the public key out-of-band and protects against server MITM key swaps.
     *
     * @param recipientUsername       Entered or chosen recipient
     * @param isFingerprintConfirmed User verification checkbox state
     * @return True if button should be enabled, false otherwise
     */
    public boolean isCreateLinkEnabled(String recipientUsername, boolean isFingerprintConfirmed) {
        boolean hasRecipient = recipientUsername != null && !recipientUsername.trim().isEmpty();
        return hasRecipient && isFingerprintConfirmed;
    }

    /**
     * Creates an encrypted share link in a background worker thread.
     *
     * @param fileId            Target file ID
     * @param fileName          Display file name
     * @param recipientUsername Target recipient
     * @param expiryOption      Expiry duration (1 hour, 24 hours, 7 days, 30 days)
     * @param callback          Callback receiving result or error on Event Dispatch Thread
     */
    public void createLink(String fileId, String fileName, String recipientUsername, String expiryOption,
                           BiConsumer<ShareLinkResult, Exception> callback) {

        SwingWorker<ShareLinkResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ShareLinkResult doInBackground() throws Exception {
                // Record verified fingerprint in local device storage
                String fp = resolveRecipientFingerprint(recipientUsername);
                recordVerifiedFingerprint(recipientUsername, fp);

                // Call share service to register link
                return shareApi.createShareLink(fileId, fileName, recipientUsername, expiryOption);
            }

            @Override
            protected void done() {
                try {
                    ShareLinkResult result = get();
                    SwingUtilities.invokeLater(() -> callback.accept(result, null));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> callback.accept(null, ex));
                }
            }
        };

        worker.execute();
    }
}
