package com.securefileshare.client.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local trust-on-first-use (TOFU) public key fingerprint persistence service.
 * WHAT: Stores verified recipient public key fingerprints in a local properties file on the client device
 *       and detects when an incoming key does not match previously accepted records.
 * WHY: CRITICAL ZERO-KNOWLEDGE DEFENSE: Defends against server key-substitution / MITM attacks.
 *      If a compromised server or attacker attempts to swap a recipient's public key with an attacker-controlled key,
 *      this service flags the discrepancy immediately so the user can abort before encrypting files.
 */
public class KnownFingerprintsService {

    private final Path storagePath;
    private final Properties properties = new Properties();

    /**
     * Constructs the service using default ~/.securefileshare/known_fingerprints.properties storage.
     */
    public KnownFingerprintsService() {
        this(Paths.get(System.getProperty("user.home"), ".securefileshare", "known_fingerprints.properties"));
    }

    /**
     * Constructs the service using a custom file path (allows isolated unit testing).
     *
     * @param storagePath Target properties file path
     */
    public KnownFingerprintsService(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    /**
     * Loads existing fingerprint records from the local filesystem.
     */
    public synchronized void load() {
        if (Files.isRegularFile(storagePath)) {
            try (InputStream in = Files.newInputStream(storagePath)) {
                properties.load(in);
            } catch (Exception ex) {
                System.err.println("Warning: Could not read known fingerprints file: " + ex.getMessage());
            }
        }
    }

    /**
     * Persists known fingerprints to the local filesystem.
     */
    public synchronized void save() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            try (OutputStream out = Files.newOutputStream(storagePath)) {
                properties.store(out, "SecureFileShare - Locally verified recipient public key fingerprints");
            }
        } catch (Exception ex) {
            System.err.println("Warning: Could not persist known fingerprints: " + ex.getMessage());
        }
    }

    /**
     * Retrieves the saved fingerprint for a recipient username.
     *
     * @param username Target recipient
     * @return Saved SHA-256 fingerprint string, or null if unknown
     */
    public synchronized String getKnownFingerprint(String username) {
        if (username == null) return null;
        return properties.getProperty(username.toLowerCase().trim());
    }

    /**
     * Checks if the device has previously verified a key for this recipient.
     *
     * @param username Target recipient
     * @return True if a fingerprint record exists
     */
    public synchronized boolean isKnown(String username) {
        return getKnownFingerprint(username) != null;
    }

    /**
     * Compares an incoming public key fingerprint against the locally recorded fingerprint.
     * WHAT: Checks whether the key has changed since the user last shared with this recipient.
     * WHY: Detects potential server-side key tampering or unauthorized key rotation.
     *
     * @param username            Target recipient
     * @param incomingFingerprint Fingerprint delivered from server / key registry
     * @return True if key is known AND does not match the incoming value (POTENTIAL ATTACK)
     */
    public synchronized boolean hasKeyChanged(String username, String incomingFingerprint) {
        String saved = getKnownFingerprint(username);
        if (saved == null) {
            // First time seeing this user: no prior record to conflict with
            return false;
        }

        // Security Decision: Compare fingerprints case-insensitively
        // Why: Hexadecimal fingerprints can be rendered uppercase or lowercase without semantic difference
        return !saved.trim().equalsIgnoreCase(incomingFingerprint != null ? incomingFingerprint.trim() : "");
    }

    /**
     * Records or updates a verified fingerprint for a recipient.
     *
     * @param username    Recipient username
     * @param fingerprint Verified SHA-256 fingerprint
     */
    public synchronized void saveFingerprint(String username, String fingerprint) {
        if (username == null || fingerprint == null) return;
        properties.setProperty(username.toLowerCase().trim(), fingerprint.trim());
        save();
    }
}
