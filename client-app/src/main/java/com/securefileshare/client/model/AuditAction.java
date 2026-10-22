package com.securefileshare.client.model;

/**
 * Enumeration of auditable security operations on the SecureFileShare platform.
 * WHAT: Enumerates UPLOAD, SHARE, DOWNLOAD, DENIED, and REVOKE actions.
 * WHY: Strict compliance with Phase C6 audit table contract.
 */
public enum AuditAction {
    UPLOAD("UPLOAD"),
    SHARE("SHARE"),
    DOWNLOAD("DOWNLOAD"),
    DENIED("DENIED"),
    REVOKE("REVOKE");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
