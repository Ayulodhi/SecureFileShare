package com.securefileshare.client.model;

/**
 * Lifecycle status of an encrypted file share permission.
 * WHAT: Enumerates ACTIVE, EXPIRED, and REVOKED states.
 * WHY: Governs whether recipient wrapped keys remain valid and decryptable.
 */
public enum ShareStatus {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    REVOKED("Revoked");

    private final String displayName;

    ShareStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
