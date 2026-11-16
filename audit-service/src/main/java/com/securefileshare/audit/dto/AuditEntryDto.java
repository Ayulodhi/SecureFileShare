package com.securefileshare.audit.dto;

import com.securefileshare.audit.entity.AuditRecord;
import java.time.Instant;

/**
 * DTO representing an audit entry.
 * WHAT: Exposes id, timestamp, actor, action, file name, hash, and previous hash.
 * WHY: Conforms precisely to web-app and desktop client AuditEntry interface.
 */
public class AuditEntryDto {

    private Long id;
    private Instant timestamp;
    private String actor;
    private String action;
    private String file;
    private String hash;
    private String previousHash;

    public AuditEntryDto() {}

    public AuditEntryDto(Long id, Instant timestamp, String actor, String action, String file, String hash, String previousHash) {
        this.id = id;
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.file = file;
        this.hash = hash;
        this.previousHash = previousHash;
    }

    public static AuditEntryDto fromEntity(AuditRecord record) {
        return new AuditEntryDto(
                record.getId(),
                record.getTimestamp(),
                record.getActor(),
                record.getAction(),
                record.getFileName(),
                record.getHash(),
                record.getPreviousHash()
        );
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getFile() { return file; }
    public String getHash() { return hash; }
    public String getPreviousHash() { return previousHash; }
}
