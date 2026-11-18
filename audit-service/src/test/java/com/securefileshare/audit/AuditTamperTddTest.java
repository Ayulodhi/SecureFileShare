package com.securefileshare.audit;

import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.entity.AuditRecord;
import com.securefileshare.audit.repository.AuditRepository;
import com.securefileshare.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AuditTamperTddTest extends BaseAuditIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
    }

    @Test
    void testTamperedAuditRowDetection() {
        auditService.logEvent(new LogEventRequest("alice", "LOGIN", "—"));
        auditService.logEvent(new LogEventRequest("alice", "UPLOAD", "secret.pdf"));

        List<AuditRecord> records = auditRepository.findAllByOrderByIdAsc();
        assertThat(records).hasSize(2);

        // Tamper with record #1 hash using reflection
        AuditRecord rec1 = records.get(0);
        ReflectionTestUtils.setField(rec1, "hash", "000000000000000000000000000000000000000000000000000000000000dead");
        auditRepository.save(rec1);

        var verifyResult = auditService.verifyChain();
        assertThat(verifyResult.isValid()).isFalse();
        assertThat(verifyResult.getBrokenEntryId()).isEqualTo(rec1.getId());
        assertThat(verifyResult.getMessage()).contains("hash has been modified");
    }
}
