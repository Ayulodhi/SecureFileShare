package com.securefileshare.client.model;

/**
 * Five sequential security checks executed when receiving and verifying an encrypted file.
 * WHAT: Enumerates link signature & expiry (Valid), private key unwrap (Done), ECDSA sender signature (Verified),
 *       decryption & SHA-256 match (Match), and pre-save malware re-scan (Clean).
 * WHY: Strict compliance with Phase C5 check pipeline.
 */
public enum ReceiveCheck {
    CHECK_1_LINK("Link signature and expiry", "Valid"),
    CHECK_2_UNWRAP("Key unwrapped with your private key", "Done"),
    CHECK_3_SIGNATURE("Sender signature ECDSA", "Verified"),
    CHECK_4_DECRYPT_HASH("Decrypted and SHA-256 match", "Match"),
    CHECK_5_MALWARE_RESCAN("Malware re-scan before saving", "Clean");

    private final String title;
    private final String passedChipLabel;

    ReceiveCheck(String title, String passedChipLabel) {
        this.title = title;
        this.passedChipLabel = passedChipLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getPassedChipLabel() {
        return passedChipLabel;
    }
}
