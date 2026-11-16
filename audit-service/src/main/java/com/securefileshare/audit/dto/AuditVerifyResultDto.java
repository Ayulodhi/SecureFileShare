package com.securefileshare.audit.dto;

/**
 * DTO representing hash chain verification outcome.
 * WHAT: Exposes valid boolean, checked entry count, broken entry ID if any, and message.
 * WHY: Conforms precisely to web-app AuditVerifyResult interface.
 */
public class AuditVerifyResultDto {

    private boolean valid;
    private int checkedCount;
    private Long brokenEntryId;
    private String message;

    public AuditVerifyResultDto() {}

    public AuditVerifyResultDto(boolean valid, int checkedCount, Long brokenEntryId, String message) {
        this.valid = valid;
        this.checkedCount = checkedCount;
        this.brokenEntryId = brokenEntryId;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public int getCheckedCount() { return checkedCount; }
    public Long getBrokenEntryId() { return brokenEntryId; }
    public String getMessage() { return message; }
}
