package com.securefileshare.client.api;

import com.securefileshare.client.model.AuditAction;
import com.securefileshare.client.model.AuditEntryDto;
import com.securefileshare.client.model.AuditVerifyResult;

import java.util.List;

/**
 * Service API interface for querying and cryptographically verifying tamper-evident audit logs.
 * WHAT: Declares contract for paginated filtered log retrieval and end-to-end hash chain verification.
 * WHY: Decouples the Admin AuditPanel from Mock vs live Spring Boot audit-service implementations.
 */
public interface AuditApi {

    /**
     * Retrieves paginated audit entries with optional actor, action, and date range filters.
     *
     * @param actor     Optional actor username filter (null or empty for all)
     * @param action    Optional action filter (null for all actions)
     * @param dateRange Date range filter string ("All time", "Last 24 hours", "Last 7 days", "Last 30 days")
     * @param page      1-based page index
     * @param pageSize  Number of entries per page (e.g. 50)
     * @return List of matching AuditEntryDto items
     * @throws Exception on service failure
     */
    List<AuditEntryDto> getAuditEntries(String actor, AuditAction action, String dateRange, int page, int pageSize) throws Exception;

    /**
     * Returns total number of entries matching the specified filter criteria.
     *
     * @param actor     Optional actor username filter
     * @param action    Optional action filter
     * @param dateRange Date range filter string
     * @return Total count of matching entries
     * @throws Exception on service failure
     */
    int getTotalCount(String actor, AuditAction action, String dateRange) throws Exception;

    /**
     * Cryptographically verifies the SHA-256 hash chain from genesis to head block.
     *
     * @return AuditVerifyResult indicating valid chain or identifying the broken block
     * @throws Exception on verification failure
     */
    AuditVerifyResult verifyChain() throws Exception;

    /**
     * Toggles tamper simulation mode for testing red error states.
     *
     * @param tampered True to inject a modified block hash
     */
    void setTamperMode(boolean tampered);

    /**
     * Checks if tamper simulation mode is active.
     *
     * @return True if chain tampering is simulated
     */
    boolean isTamperMode();
}
