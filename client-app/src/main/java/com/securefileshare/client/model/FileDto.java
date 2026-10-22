package com.securefileshare.client.model;

/**
 * Data Transfer Object representing an encrypted file owned by the user.
 * WHAT: Contains file ID, filename, formatted size, scan status, and sharing summary.
 * WHY: Powers the "My files" screen table without exposing sensitive ciphertext.
 */
public class FileDto {

    private final String id;
    private final String name;
    private final String formattedSize;
    private final ScanStatus scanStatus;
    private final String sharedWith;
    private final boolean canShare;

    public FileDto(String id, String name, String formattedSize, ScanStatus scanStatus, String sharedWith) {
        this.id = id;
        this.name = name;
        this.formattedSize = formattedSize;
        this.scanStatus = scanStatus;
        this.sharedWith = sharedWith;
        // Security rule: Quarantined files must never be shared
        // Why: Prevents propagation of infected payloads across users
        this.canShare = (scanStatus == ScanStatus.CLEAN);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public String getSharedWith() {
        return sharedWith;
    }

    public boolean isCanShare() {
        return canShare;
    }
}
