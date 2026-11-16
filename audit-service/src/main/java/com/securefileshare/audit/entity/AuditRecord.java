package com.securefileshare.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity mapping to the AUDIT_ENTRIES database table.
 * WHAT: Persists immutable, hash-chained audit entries.
 * WHY: Provides tamper-evident compliance logging.
 */
@Entity
@Table(name = "audit_entries")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    public AuditRecord() {}

    public AuditRecord(Instant timestamp, String actor, String action, String fileName, String hash, String previousHash) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.fileName = fileName;
        this.hash = hash;
        this.previousHash = previousHash;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getFileName() { return fileName; }
    public String getHash() { return hash; }
    public String getPreviousHash() { return previousHash; }
}
