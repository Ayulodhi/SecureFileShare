package com.securefileshare.client.model;

/**
 * Data Transfer Object representing an individual record in the tamper-evident audit log.
 * WHAT: Contains sequence ID, timestamp, actor, action (UPLOAD, SHARE, DOWNLOAD, DENIED, REVOKE),
 *       file name, outcome result, cryptographic hash, and parent block hash.
 * WHY: Powers the Admin audit table and forms the basis of SHA-256 hash-chain verification.
 */
public class AuditEntryDto {

    private final long id;
    private final String timestamp;
    private final String actor;
    private final AuditAction action;
    private final String file;
    private final String result;
    private final String hash;
    private final String previousHash;

    public AuditEntryDto(long id, String timestamp, String actor, AuditAction action,
                         String file, String result, String hash, String previousHash) {
        this.id = id;
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.file = file;
        this.result = result;
        this.hash = hash;
        this.previousHash = previousHash;
    }

    public long getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getFile() {
        return file;
    }

    public String getResult() {
        return result;
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    /**
     * Formats full 64-hex SHA-256 digest into compact monospace column representation.
     *
     * @return 12-char prefix with ellipsis
     */
    public String getShortHash() {
        if (hash == null || hash.length() < 12) {
            return hash != null ? hash : "—";
        }
        return hash.substring(0, 12) + "...";
    }
}
