package com.securefileshare.auth.entity;

/**
 * Enumeration of system authorization roles.
 * WHAT: Distinguishes between standard end-users and administrators.
 * WHY: Enforces role-based access control (RBAC). Admin accounts cannot be self-registered.
 */
public enum Role {
    /**
     * Regular participant authorized to upload, share, and receive encrypted files.
     */
    SENDER_RECIPIENT,

    /**
     * System administrator authorized for system oversight and quarantine management.
     */
    ADMIN
}
