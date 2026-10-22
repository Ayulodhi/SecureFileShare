package com.securefileshare.file.entity;

/**
 * Lifecycle status of an uploaded encrypted file.
 * WHAT: Enumerates processing states from initial upload through malware scanning.
 * WHY: Tracks whether a file is safe for distribution while maintaining zero-knowledge isolation.
 */
public enum FileStatus {

    /**
     * File was received and verified cryptographically, but not yet evaluated by the malware scanner.
     */
    PENDING_SCAN,

    /**
     * File was evaluated by the malware scanner and verified benign.
     */
    CLEAN,

    /**
     * File was evaluated by the malware scanner and determined to be malicious.
     */
    INFECTED,

    /**
     * File was quarantined due to malware detection or policy violation.
     */
    QUARANTINED,

    /**
     * Malware scan worker failed during processing.
     */
    FAILED
}
