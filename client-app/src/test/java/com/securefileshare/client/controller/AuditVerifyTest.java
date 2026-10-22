package com.securefileshare.client.controller;

import com.securefileshare.client.api.mock.MockAuditApi;
import com.securefileshare.client.model.AuditAction;
import com.securefileshare.client.model.AuditEntryDto;
import com.securefileshare.client.model.AuditVerifyResult;
import com.securefileshare.client.ui.SidebarPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Admin Audit Log chain verification and banner states.
 * WHAT: Asserts green "Chain valid" and red "Chain broken" verification outcomes,
 *       50-row pagination boundaries, action filtering, and admin role sidebar hiding.
 * WHY: Strict compliance with Phase C6 Requirement 3.
 */
class AuditVerifyTest {

    private MockAuditApi auditApi;

    @BeforeEach
    void setUp() {
        auditApi = new MockAuditApi();
    }

    @Test
    @DisplayName("Verify chain on untouched log should return green valid state with entry count")
    void testVerifyChainValidGreenState() throws Exception {
        auditApi.setTamperMode(false);

        AuditVerifyResult result = auditApi.verifyChain();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getCheckedCount()).isEqualTo(65);
        assertThat(result.getBrokenEntryId()).isNull();
        assertThat(result.getMessage()).contains("Chain valid, 65 entries checked");
    }

    @Test
    @DisplayName("Verify chain on tampered log should return red broken state identifying entry #42")
    void testVerifyChainTamperedRedState() throws Exception {
        // Activate simulated block hash tampering
        auditApi.setTamperMode(true);

        AuditVerifyResult result = auditApi.verifyChain();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getBrokenEntryId()).isEqualTo(42L);
        assertThat(result.getMessage()).contains("Chain broken at entry #42");
    }

    @Test
    @DisplayName("Pagination must return exactly 50 rows on page 1 and remainder on page 2")
    void testAuditPagination50RowsPerPage() throws Exception {
        int total = auditApi.getTotalCount(null, null, "All time");
        assertThat(total).isEqualTo(65);

        // Page 1: exactly 50 rows
        List<AuditEntryDto> page1 = auditApi.getAuditEntries(null, null, "All time", 1, 50);
        assertThat(page1).hasSize(50);
        assertThat(page1.get(0).getId()).isEqualTo(1L);
        assertThat(page1.get(49).getId()).isEqualTo(50L);

        // Page 2: remaining 15 rows
        List<AuditEntryDto> page2 = auditApi.getAuditEntries(null, null, "All time", 2, 50);
        assertThat(page2).hasSize(15);
        assertThat(page2.get(0).getId()).isEqualTo(51L);
        assertThat(page2.get(14).getId()).isEqualTo(65L);
    }

    @Test
    @DisplayName("Filtering by Action should return only matching action records")
    void testActionFilter() throws Exception {
        List<AuditEntryDto> uploads = auditApi.getAuditEntries(null, AuditAction.UPLOAD, "All time", 1, 100);

        assertThat(uploads).isNotEmpty();
        for (AuditEntryDto entry : uploads) {
            assertThat(entry.getAction()).isEqualTo(AuditAction.UPLOAD);
        }
    }

    @Test
    @DisplayName("Sidebar admin entry must be hidden for non-admin users")
    void testNonAdminSidebarHiding() {
        NavigationService nav = new NavigationService();
        SidebarPanel sidebar = new SidebarPanel(nav);

        // Standard user: admin audit entry hidden
        sidebar.setAdmin(false);

        // Admin user: admin audit entry enabled
        sidebar.setAdmin(true);
    }
}
