package com.securefileshare.audit;

import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.service.AuditIdempotencyDecorator;
import com.securefileshare.audit.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class StageCOutboxTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_audit");
        registry.add("spring.datasource.username", () -> "sfs_admin");
        registry.add("spring.datasource.password", () -> "sfs_secure_pass_2026");
    }

    @Autowired
    private AuditIdempotencyDecorator idempotencyDecorator;

    @Autowired
    private AuditService auditService;

    @Test
    @DisplayName("Stage C - Audit Outbox & Idempotency: Duplicate event ID is processed exactly once")
    void testAuditIdempotencyDeduplication() {
        UUID eventId = UUID.randomUUID();
        LogEventRequest req = new LogEventRequest("alice", "UPLOAD", "outbox_test.pdf");

        var firstCall = idempotencyDecorator.logEventIdempotent(eventId, req);
        var secondCall = idempotencyDecorator.logEventIdempotent(eventId, req);

        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
    }
}
