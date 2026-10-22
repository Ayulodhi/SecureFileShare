package com.securefileshare.client.model;

/**
 * Result of client-side pre-upload malware scanning (YARA + ClamAV).
 * WHAT: Clean or Malware Detected.
 * WHY: Critical zero-knowledge rule: If malware is found, upload is aborted at Step 1 and nothing is sent.
 */
public enum ScanDisposition {
    CLEAN,
    MALWARE_FOUND
}
