package com.securefileshare.audit;

import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.entity.AuditRecord;
import com.securefileshare.audit.repository.AuditRepository;
import com.securefileshare.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AuditServiceUnitTest {

    private AuditRepository auditRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditRepository = Mockito.mock(AuditRepository.class);
        auditService = new AuditService(auditRepository);
    }

    @Test
    void testDelimiterPreventsConcatenationCollision() {
        when(auditRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditRepository.save(any(AuditRecord.class))).thenAnswer(inv -> {
            AuditRecord r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 1L);
            return r;
        });

        var res1 = auditService.logEvent(new LogEventRequest("ab", "c", "file.txt"));
        var res2 = auditService.logEvent(new LogEventRequest("a", "bc", "file.txt"));

        assertThat(res1.getHash()).isNotEqualTo(res2.getHash());
    }
}
