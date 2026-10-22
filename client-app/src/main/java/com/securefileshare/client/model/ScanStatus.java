package com.securefileshare.client.model;

/**
 * Antivirus and malware scan disposition for client-side encrypted files.
 * WHAT: Enumerates clean, quarantined, and pending scan states.
 * WHY: Enforces zero-knowledge safety rule: Only Clean files can be decrypted or shared.
 *      Quarantined files are blocked from distribution.
 */
public enum ScanStatus {
    CLEAN("Clean"),
    QUARANTINED("Quarantined"),
    PENDING("Pending Scan");

    private final String displayName;

    ScanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
