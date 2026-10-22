package com.securefileshare.client.model;

/**
 * Data Transfer Object representing an encrypted file shared with the current user.
 * WHAT: Captures file name, size, sender identity, expiration window, and scan status.
 * WHY: Powers the "Shared with me" panel table.
 */
public class SharedFileDto {

    private final String fileId;
    private final String fileName;
    private final String size;
    private final String sharedBy;
    private final String expiresIn;
    private final ScanStatus scanStatus;

    public SharedFileDto(String fileId, String fileName, String size, String sharedBy, String expiresIn, ScanStatus scanStatus) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.size = size;
        this.sharedBy = sharedBy;
        this.expiresIn = expiresIn;
        this.scanStatus = scanStatus;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSize() {
        return size;
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }
}
