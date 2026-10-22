package com.securefileshare.client.model;

/**
 * Result of executing the cryptographic hash chain verification.
 * WHAT: Conveys whether the entire tamper-evident blockchain/hash chain is valid,
 *       the total count of checked entries, and the exact ID of any broken entry.
 * WHY: Governs the banner state on the Admin Audit screen (Green for valid, Red for broken).
 */
public class AuditVerifyResult {

    private final boolean valid;
    private final int checkedCount;
    private final Long brokenEntryId;
    private final String message;

    public AuditVerifyResult(boolean valid, int checkedCount, Long brokenEntryId, String message) {
        this.valid = valid;
        this.checkedCount = checkedCount;
        this.brokenEntryId = brokenEntryId;
        this.message = message;
    }

    public static AuditVerifyResult valid(int checkedCount) {
        return new AuditVerifyResult(true, checkedCount, null, "Chain valid, " + checkedCount + " entries checked");
    }

    public static AuditVerifyResult broken(long brokenEntryId, int checkedCount) {
        return new AuditVerifyResult(false, checkedCount, brokenEntryId, "Chain broken at entry #" + brokenEntryId);
    }

    public boolean isValid() {
        return valid;
    }

    public int getCheckedCount() {
        return checkedCount;
    }

    public Long getBrokenEntryId() {
        return brokenEntryId;
    }

    public String getMessage() {
        return message;
    }
}
