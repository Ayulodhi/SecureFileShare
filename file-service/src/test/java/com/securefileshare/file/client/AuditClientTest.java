package com.securefileshare.file.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TDD Test for Audit Event Publishing Resilience (Task 4).
 * WHAT: Verifies that when audit-service is down or unreachable, publishing an audit event
 *       does not block or throw exceptions to the primary user request.
 */
public class AuditClientTest {

    @Test
    void testAuditPublishingDoesNotBlockWhenAuditServiceIsDown() {
        AuditServiceClient auditClient = new AuditServiceClient("http://localhost:1", "sfs_service_audit_secret_2026", new ObjectMapper());

        assertThatCode(() -> {
            auditClient.logEvent("alice", "UPLOAD", "test.txt");
            // Give async task a brief moment to complete/catch exception
            Thread.sleep(100);
        }).doesNotThrowAnyException();
    }
}
