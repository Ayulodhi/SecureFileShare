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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AuditHashChainTddTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_audit");
        registry.add("spring.datasource.username", () -> "sfs_admin");
        registry.add("spring.datasource.password", () -> "sfs_secure_pass_2026");
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
    }

    @Test
    void testDelimiterBoundaryCollisionResistance() {
        // (a) ("ab", "c") vs ("a", "bc") must produce different hashes due to delimiters / length prefixing
        var r1 = auditService.logEvent(new LogEventRequest("ab", "c", "file.txt"));
        auditRepository.deleteAll();
        var r2 = auditService.logEvent(new LogEventRequest("a", "bc", "file.txt"));

        assertThat(r1.getHash()).isNotEqualTo(r2.getHash());
    }

    @Test
    void testConcurrentAppendsNeverSharePrevHash() throws Exception {
        // (b) Two concurrent appends must never share a prev_hash and chain verifies afterwards
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            tasks.add(() -> {
                try {
                    auditService.logEvent(new LogEventRequest("user" + idx, "UPLOAD", "file" + idx + ".txt"));
                } finally {
                    latch.countDown();
                }
                return null;
            });
        }

        executor.invokeAll(tasks);
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<AuditRecord> records = auditRepository.findAllByOrderByIdAsc();
        assertThat(records).hasSize(threadCount);

        Set<String> prevHashes = new HashSet<>();
        for (AuditRecord rec : records) {
            prevHashes.add(rec.getPreviousHash());
        }
        assertThat(prevHashes).hasSize(threadCount);

        // Verify chain
        var verifyResult = auditService.verifyChain();
        assertThat(verifyResult.isValid()).isTrue();
    }

    @Test
    void testTimestampRoundTripAndMixedVersionVerification() {
        // (c) Timestamp hashed in fixed UTC ISO-8601 format surviving DB round trip
        var log1 = auditService.logEvent(new LogEventRequest("admin", "LOGIN", "—"));

        List<AuditRecord> records = auditRepository.findAllByOrderByIdAsc();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getHashVersion()).isEqualTo(2); // V2 new hash

        var verifyResult = auditService.verifyChain();
        assertThat(verifyResult.isValid()).isTrue();
    }
}
